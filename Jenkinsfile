#!/usr/bin/env groovy

pipeline {
    agent any

    triggers {
        // Listen for GitLab push events on all branches
        gitlab(
            triggerOnPush: true,
            branchFilterType: 'All',
            secretToken: '7b0b680f36c40cf9fe91652dac54411f'
        )
    }

    options {
        // Keep up to 10 build logs
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // Disable concurrent builds to avoid race conditions and aboard previous builds
        disableConcurrentBuilds(abortPrevious: true)
        // Set a timeout of 1 hour
        timeout(time: 1, unit: 'HOURS')
        // Add timestamps to build output
        timestamps()
    }

    environment {
        // Set environment variables used in the pipeline
        JAVA_HOME = '/opt/jdk-18' // JDK 18 for build
        JENKINS_CREDENTIALS = 'GA-AWS'
        AWS_ECR_REGION = 'ap-east-1' // Hong Kong
        AWS_ECR_URL = '634937900606.dkr.ecr.ap-east-1.amazonaws.com/ga-vendor-api-service'

        AWS_ECS_REGION = 'ap-east-1' // Hong Kong
        AWS_ECS_COMPATIBILITY = 'FARGATE'
        AWS_ECS_NETWORK_MODE = 'awsvpc'
        AWS_ECS_CPU = '2048'
        AWS_ECS_MEMORY = '4096'
        AWS_ECS_EXECUTION_ROL = 'arn:aws:iam::634937900606:role/devops_ecs_cicd'
        AWS_ECS_TASK_DEFINITION = ''
        AWS_ECS_CLUSTER = ''
        AWS_ECS_SERVICE = ''

        SONAR_PROJECTKEY = 'game-aggregator'
        SONAR_HOST_URL = 'http://192.168.88.112:9000'
        SONAR_LOGIN = credentials('sonar_token')

        QA_LOGIN_SERVER = 'root@35.77.164.118'
        PORTAINER_SERVICE_NAME = 'vendor-api_main-service'
    }

    stages {
        stage('SonarCube') {
            when {
                branch 'stg'
            }
            steps {
                sh 'mvn clean verify sonar:sonar -Dsonar.projectKey=$SONAR_PROJECTKEY -Dsonar.host.url=$SONAR_HOST_URL -Dsonar.login=$SONAR_LOGIN -DskipTests=true'
            }
        }

        stage('Build Project') {
            when {
                not {
                    branch 'main'
                }
            }
            steps {
                script {
                    String couchbase_cert_file_id = getCouchbaseCertId(env.BRANCH_NAME)
                    String pom_file = getPomFile(env.BRANCH_NAME)

                    withCredentials([file(credentialsId: "${couchbase_cert_file_id}", variable: 'SECRET_FILE')]) {
                        configFileProvider([configFile(fileId: 'version_num', variable: 'VERSION_NUMBER')]) {
                            String VERSION_NUMBER = readFile(VERSION_NUMBER).trim()
                            sh 'cp -rf $SECRET_FILE ./game_aggregator-root-certificate.pem'
                            sh "mvn versions:set -DnewVersion=$VERSION_NUMBER"
                            sh "mvn clean package spring-boot:repackage -U -f ${pom_file} -DskipTests"
                        }
                    }
                }
            }
        }

        stage('Build Docker Image') {
            when {
                not {
                    branch 'qa'
                }
            }
            steps {
                script {
                    String packageVersion = getRepoTag(env.BRANCH_NAME)
                    docker.build("${AWS_ECR_URL}:${packageVersion}", ' .')
                }
            }
        }

        stage('Copy jar file to QA & Build Docker Image For QA & Deploy in QA Server') {
            when {
                branch 'qa'
            }
            steps {
                script {
                    sshagent(credentials: ['tokyo_key']) {
                        // Copy jar file to Server
                        sh "scp -o StrictHostKeyChecking=no ./target/*.jar ${QA_LOGIN_SERVER}:/root/vendor-api/app.jar"

                        sh "ssh -t -o StrictHostKeyChecking=no ${QA_LOGIN_SERVER} 'docker build -t local-ga-vendor-api-service:qa /root/vendor-api'"

                        sh "ssh -t -o StrictHostKeyChecking=no ${QA_LOGIN_SERVER} 'docker service update --force --image local-ga-vendor-api-service:qa ${PORTAINER_SERVICE_NAME}'"
                        // TODO: Temporary solution, need to remove when done callback migrate (This is /vendor service)
                        sh "ssh -t -o StrictHostKeyChecking=no ${QA_LOGIN_SERVER} 'docker service update --force --image local-ga-vendor-api-service:qa vendor-api_sub-service'"
                    }
                }
            }
        }

        stage('Push Docker Image') {
            when {
                not {
                    branch 'qa'
                }
            }
            steps {
                // Build and push a Docker image to Amazon ECR
                withAWS(region: "${AWS_ECR_REGION}", credentials: "${JENKINS_CREDENTIALS}") {
                    script {
                        String packageVersion = getRepoTag(env.BRANCH_NAME)
                        String login = ecrLogin()
                        sh("#!/bin/sh -e\n${login}") // hide logging
                        docker.image("${AWS_ECR_URL}:${packageVersion}").push()
                    }
                }
            }
        }

        stage('Deploy in ECS') {
            when {
                not {
                    branch 'qa'
                }
                not {
                    branch 'main'
                }
            }
            steps {
                // Update a task definition with a new Docker image and deploy it to Amazon ECS
                withAWS(region: "${AWS_ECS_REGION}", credentials: "${JENKINS_CREDENTIALS}") {
                    script {
                        String branchName = env.BRANCH_NAME
                        String packageVersion = getRepoTag(branchName)

                        withEnv(getECSConfig(branchName)) {
                            configFileProvider([configFile(fileId: "${branchName}_td", variable: 'taskDefinitionPath')]) {
                                updateContainerDefinitionJsonWithImageVersion(packageVersion, taskDefinitionPath)
                                sh("aws ecs register-task-definition --region ${AWS_ECS_REGION} --family ${AWS_ECS_TASK_DEFINITION} --execution-role-arn ${AWS_ECS_EXECUTION_ROL} --requires-compatibilities ${AWS_ECS_COMPATIBILITY} --network-mode ${AWS_ECS_NETWORK_MODE} --cpu ${AWS_ECS_CPU} --memory ${AWS_ECS_MEMORY} --container-definitions file://${taskDefinitionPath}")
                                String taskRevision = sh(script: "aws ecs describe-task-definition --task-definition ${AWS_ECS_TASK_DEFINITION} | grep -oP '\"revision\": \\K\\d+'", returnStdout: true)
                                sh("aws ecs update-service --cluster ${AWS_ECS_CLUSTER} --service ${AWS_ECS_SERVICE} --task-definition ${AWS_ECS_TASK_DEFINITION}:${taskRevision}")
                            }
                        }
                    }
                }
            }
        }

        stage('Tagging') {
            when {
                branch 'stg'
            }
            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: 'gitlab-root', gitToolName: 'Default')]) {
                        configFileProvider([configFile(fileId: 'version_num', variable: 'VERSION_NUMBER')]) {
                            String VERSION_NUMBER = readFile(VERSION_NUMBER).trim()
                            String commitMessage = sh(returnStdout: true, script: 'git log --format=%B -n 1').trim()
                            String versionTag = "$VERSION_NUMBER.${env.BUILD_NUMBER}"
                            sh "git tag -a ${versionTag} -m '${commitMessage}'"
                            sh "git push origin ${versionTag}"
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                switch (env.BRANCH_NAME) {
                case 'main':
                case 'stg':
                case 'qa':
                case 'pt':
                case 'devops':
                        discordSend description: "${currentBuild.currentResult}: ${env.JOB_NAME} #${currentBuild.number}", title: 'Pipeline Status', webhookURL: 'https://discord.com/api/webhooks/1055669297151746049/6hhQcW2n2z5FfiDCzKNioMDV7bMm10HyaSebl4CqqDUXpbSU2L9R5-HoVuNu7sL9NIsl?thread_id=1113328150210949130', link: currentBuild.absoluteUrl, result: currentBuild.currentResult, showChangeset: true
                        break
                }
            }
        }
    }
}

// A function to update the container definition JSON file with the new Docker image version
void updateContainerDefinitionJsonWithImageVersion(String packageVersion, String taskDefinitionPath) {
    List containerDefinitionJson = readJSON file: taskDefinitionPath, returnPojo: true
    containerDefinitionJson[0]['image'] = "${AWS_ECR_URL}:${packageVersion}".inspect()
    echo "task definition JSON: ${containerDefinitionJson}"
    writeJSON file: taskDefinitionPath, json: containerDefinitionJson
}

String getRepoTag(String branchName) {
    String packageVersion = 'dev'

    switch (branchName) {
        case 'main':
            packageVersion = 'latest'
            break
        case 'stg':
            packageVersion = 'stg'
            break
        case 'qa':
            packageVersion = 'qa'
            break
        case 'pt':
            packageVersion = 'pt'
            break
        case 'devops':
            packageVersion = 'devo'
            break
    }

    return packageVersion
}

def getECSConfig(String branchName) {
    def config = []
    switch (branchName) {
        case 'stg':
            config = [
                'AWS_ECS_CLUSTER=stg',
                'AWS_ECS_SERVICE=vendor-api-service',
                'AWS_ECS_TASK_DEFINITION=stg-ga_vendor_api-td'
            ]
            break
        case 'pt':
            config = [
                'AWS_ECS_CLUSTER=pt',
                'AWS_ECS_SERVICE=vendor-api-service',
                'AWS_ECS_TASK_DEFINITION=pt-vendor-api-service-td'
            ]
            break
    }
    return config
}

String getCouchbaseCertId(String branchName) {
    String file = ''

    switch (branchName) {
        case 'main':
            file = 'prd_couchbase_cert_file'
            break
        case 'stg':
        case 'qa':
        case 'pt':
            file = 'couchbase_cert_file'
            break
    }

    return file
}

String getPomFile(String branchName) {
    String file = './pom.xml'

    switch (branchName) {
        case 'main':
            file = './pom-deploy.xml'
            break
        case 'stg':
        case 'qa':
        case 'pt':
            file = './pom.xml'
            break
    }

    return file
}

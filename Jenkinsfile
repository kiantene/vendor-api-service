pipeline {
    agent any

    tools {
        jdk 'Java18'
        maven 'Maven3.8.8'
    }

    options {
        disableConcurrentBuilds(abortPrevious: true)
        timestamps()
    }

    stages {
        stage('Read Config') {
            steps {
                script {
                    configFileProvider([configFile(fileId: 'common_config', variable: 'CONFIG_FILE')]) {
                        def props = readProperties file: CONFIG_FILE

                        props.each { key, value ->
                            env."${key}" = value.trim()
                        }
                    }

                    String configName = "${BRANCH_NAME}_config"

                    configFileProvider([configFile(fileId: configName, variable: 'CONFIG_FILE')]) {
                        def props = readProperties file: CONFIG_FILE

                        props.each { key, value ->
                            env."${key}" = value.trim()
                        }
                    }
                }
            }
        }

        stage('Tagging') {
            when {
                environment name: 'TRIGGER_TAGGING', value: 'true'
            }

            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: "${GIT_CREDENTIALS_ID}", gitToolName: 'Default')]) {
                        String commitMessage = sh(returnStdout: true, script: 'git log --format=%B -n 1').trim()
                        String branchName = BRANCH_NAME
                        String versionTag = getVersionTag(branchName)

                        sh "mvn versions:set -DnewVersion=$versionTag"

                        if (TRIGGER_PUSH_TAGGING == 'true') {
                            sh "git tag -a ${versionTag} -m '${commitMessage}'"
                            sh "git push origin ${versionTag}"
                        }

                        if (branchName == 'preprod') {
                            def userInput = input message: 'new version?', ok: 'Proceed', parameters: [choice(name: 'Confirm', choices: 'yes\nno', description: 'Confirm new version update?')]

                            if (userInput == 'yes') {
                                versionTag = getVersionTag(branchName, true)
                                sh "mvn versions:set -DnewVersion=$versionTag"

                                sh 'git add .'
                                sh "git commit -m 'Update version to $versionTag'"
                                sh 'git push origin HEAD:preprod'
                            }
                        }
                    }
                }
            }
        }

        stage('SonarQube') {
            when {
                environment name: 'TRIGGER_SONARQUBE', value: 'true'
            }

            steps {
                withCredentials([string(credentialsId: "${SONAR_CREDENTIALS_ID}", variable: 'SONAR_TOKEN')]) {
                    sh '''
                    mvn clean verify sonar:sonar \
                        -Dmaven.test.skip \
                        -Dsonar.projectKey=$SONAR_PROJECTKEY \
                        -Dsonar.projectName=$SONAR_PROJECTNAME \
                        -Dsonar.host.url=$SONAR_HOST_URL \
                        -Dsonar.token=$SONAR_TOKEN;
                    '''
                }
            }
        }

        stage('Generate Profile Env') {
            when {
                environment name: 'TRIGGER_GENERATE_ENV', value: 'true'
            }

            steps {
                script {
                    String configName = "${BRANCH_NAME}_env"

                    configFileProvider([configFile(fileId: configName, variable: 'CONFIG_FILE')]) {
                        def props = readFile file: CONFIG_FILE

                        writeFile file: "src/main/resources/application-${PROFILE_NAME}.properties", text: props.toString()
                    }
                }
            }
        }

        stage('Build Project') {
            steps {
                script {
                    withCredentials([file(credentialsId: "${COUCHBASE_CREDENTIALS_ID}", variable: 'SECRET_FILE')]) {
                        sh 'cp -rf $SECRET_FILE ./game_aggregator-root-certificate.pem'
                    }

                    def appProps = readFile 'src/main/resources/application.properties'
                    def newAppProps = appProps.replaceAll(/(?m)^spring.profiles.active\s*=.*/, "spring.profiles.active=${PROFILE_NAME}")

                    if (!newAppProps.contains('spring.profiles.active')) {
                        newAppProps += "\nspring.profiles.active=${PROFILE_NAME}\n"
                    }

                    writeFile file: 'src/main/resources/application.properties', text: newAppProps
                    withMaven(maven: 'Maven3.8.8') {
                        sh 'mvn clean package spring-boot:repackage -U -Dmaven.test.skip=true'
                    }
                }
            }
        }

        stage('Build Docker Image') {
            when {
                environment name: 'TRIGGER_DOCKER_BUILD', value: 'true'
            }

            steps {
                script {
                    String branchTag = BRANCH_TAG
                    docker.build("${AWS_ECR_URL}:${branchTag}", ' .')
                }
            }
        }

        stage('Push Docker Image') {
            when {
                environment name: 'TRIGGER_DOCKER_PUSH', value: 'true'
            }

            steps {
                withAWS(region: "${AWS_ECR_REGION}", credentials: "${AWS_CREDENTIALS_ID}") {
                    script {
                        String branchName = BRANCH_NAME
                        String branchTag = BRANCH_TAG
                        String dockerTag = getDockerTag(branchName)

                        sh("#!/bin/sh -e\n${ecrLogin()}")
                        docker.image("${AWS_ECR_URL}:${branchTag}").push("${branchTag}")
                        docker.image("${AWS_ECR_URL}:${branchTag}").push("${dockerTag}")
                    }
                }
            }
        }

        stage('Transfer and Load Image') {
            when {
                environment name: 'TRIGGER_DOCKER_TRANSFER', value: 'true'
            }
            steps {
                script {
                    sshagent(credentials: ["${SERVER_SSH_CREDENTIALS_ID}"]) {
                        String branchTag = BRANCH_TAG
                        sh "docker save -o ${PORTAINER_SERVICE_NAME}.tar ${AWS_ECR_URL}:${branchTag}"
                        sh "scp -o StrictHostKeyChecking=no ${PORTAINER_SERVICE_NAME}.tar ${SERVER_SSH}:/tmp/"

                        sh "ssh -t -o StrictHostKeyChecking=no ${SERVER_SSH} 'docker load -i /tmp/${PORTAINER_SERVICE_NAME}.tar'"
                        sh "ssh -t -o StrictHostKeyChecking=no ${SERVER_SSH} 'docker service update --force --image ${AWS_ECR_URL}:${branchTag} --update-order start-first --update-delay 30s ${PORTAINER_SERVICE_NAME}'"
                    }
                }
            }
        }

        // stage('Deploy in ECS') {
        //     when {
        //         environment name: 'TRIGGER_DEPLOY_ECS', value: 'true'
        //     }
        //     steps {
        //         withAWS(region: "${AWS_ECS_REGION}", credentials: "${AWS_CREDENTIALS_ID}") {
        //             script {
        //                 sh("aws ecs update-service --cluster ${AWS_ECS_CLUSTER} --service ${AWS_ECS_SERVICE} --force-new-deployment")
        //                 sh("aws ecs wait services-stable --cluster ${AWS_ECS_CLUSTER} --services ${AWS_ECS_SERVICE}")
        //             }
        //         }
        //     }
        // }

        stage('Deploy in ECS') {
            when {
                environment name: 'TRIGGER_DEPLOY_ECS', value: 'true'
            }
            steps {
                withAWS(region: "${AWS_ECS_REGION}", credentials: "${AWS_CREDENTIALS_ID}") {
                    script {
                        String branchName = BRANCH_NAME
                        String branchTag = BRANCH_TAG

                        configFileProvider([configFile(fileId: "${branchName}_td", variable: 'taskDefinitionPath')]) {
                            updateContainerDefinitionJsonWithImageVersion(branchTag, taskDefinitionPath)
                            sh("aws ecs register-task-definition --region ${AWS_ECS_REGION} --family ${AWS_ECS_TASK_DEFINITION} --execution-role-arn ${AWS_ECS_EXECUTION_ROL} --requires-compatibilities ${AWS_ECS_COMPATIBILITY} --network-mode ${AWS_ECS_NETWORK_MODE} --cpu ${AWS_ECS_CPU} --memory ${AWS_ECS_MEMORY} --container-definitions file://${taskDefinitionPath}")
                            String taskRevision = sh(script: "aws ecs describe-task-definition --task-definition ${AWS_ECS_TASK_DEFINITION} | grep -oP '\"revision\": \\K\\d+'", returnStdout: true)
                            sh("aws ecs update-service --cluster ${AWS_ECS_CLUSTER} --service ${AWS_ECS_SERVICE} --task-definition ${AWS_ECS_TASK_DEFINITION}:${taskRevision}")
                            sh("aws ecs wait services-stable --cluster ${AWS_ECS_CLUSTER} --services ${AWS_ECS_SERVICE}")
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                if (TRIGGER_DISCORD == 'true') {
                    discordSend description: "${currentBuild.currentResult}: ${JOB_NAME} #${currentBuild.number}", title: "Pipeline ${currentBuild.fullProjectName} ${currentBuild.currentResult}", webhookURL: DISCORD_WEBHOOK_URL, link: currentBuild.absoluteUrl, result: currentBuild.currentResult, showChangeset: true
                }

                cleanWs(skipWhenFailed: true)
            }
        }
    }
}

String getDockerTag(String branchName) {
    String today = new Date().format('yyyyMMdd')

    return "${branchName}-${today}-${BUILD_NUMBER}"
}

String getVersionTag(String branchName, boolean newVersion = false) {
    String versionTag = '0.0.1'

    configFileProvider([configFile(fileId: 'version_num', variable: 'VERSION_NUMBER')]) {
        String VERSION_NUMBER = readFile(VERSION_NUMBER).trim()
        versionTag = "$VERSION_NUMBER.${BUILD_NUMBER}-${branchName}"
        switch (branchName) {
            case 'main':
                versionTag = "$VERSION_NUMBER"
                break
            case 'preprod':
                if (newVersion) {
                    versionTag = "$VERSION_NUMBER"
                } else {
                    versionTag = "$VERSION_NUMBER.${BUILD_NUMBER}-rc"
                }
                break
        }
    }

    return versionTag
}

void updateContainerDefinitionJsonWithImageVersion(String branchTag, String taskDefinitionPath) {
    List containerDefinitionJson = readJSON file: taskDefinitionPath, returnPojo: true
    containerDefinitionJson[0]['image'] = "${AWS_ECR_URL}:${branchTag}".inspect()
    writeJSON file: taskDefinitionPath, json: containerDefinitionJson
}

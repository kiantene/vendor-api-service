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
        // Disable concurrent builds to avoid race conditions
        disableConcurrentBuilds()
        // Set a timeout of 1 hour
        timeout(time: 1, unit: 'HOURS')
        // Add timestamps to build output
        timestamps()
    }

    environment {
        // Set environment variables used in the pipeline
        JENKINS_CREDENTIALS = 'GA-AWS'
        AWS_ECR_REGION = 'ap-east-1' // Hong Kong
        AWS_ECR_URL = '634937900606.dkr.ecr.ap-east-1.amazonaws.com/ga-vendor-api-service'
        AWS_ECS_REGION = 'ap-northeast-1' // Tokyo
        AWS_ECS_SERVICE = 'vendor-service'
        AWS_ECS_TASK_DEFINITION = 'ga_vendor_td'
        AWS_ECS_COMPATIBILITY = 'FARGATE'
        AWS_ECS_NETWORK_MODE = 'awsvpc'
        AWS_ECS_CPU = '4096'
        AWS_ECS_MEMORY = '8192'
        AWS_ECS_CLUSTER = 'nextgen_game_aggregator-dev-ecs-fargate'
        AWS_ECS_TASK_DEFINITION_PATH = './ecs/container-definition-update-image.json'
        AWS_ECS_EXECUTION_ROL = 'arn:aws:iam::634937900606:role/devops_ecs_cicd'
    }

    stages {
        // Define the stages of the pipeline
        stage('Push Docker Image') {
            steps {
                // Build and push a Docker image to Amazon ECR
                withAWS(region: "${AWS_ECR_REGION}", credentials: "${JENKINS_CREDENTIALS}") {
                    script {
                        String packageVersion = getRepoTag(env.BRANCH_NAME)
                        docker.build("${AWS_ECR_URL}:${packageVersion}", ' .')
                        String login = ecrLogin()
                        sh("#!/bin/sh -e\n${login}") // hide logging
                        docker.image("${AWS_ECR_URL}:${packageVersion}").push()
                    }
                }
            }
        }

        stage('Deploy in Alibaba Cloud') {
            when {
                branch 'qa'
            }
            steps {
                withAWS(region: "${AWS_ECR_REGION}", credentials: "${JENKINS_CREDENTIALS}") {
                    script {
                        String password = sh(script: 'aws ecr get-login-password --region ap-east-1', returnStdout: true).trim()

                        sshagent(credentials: ['CD_PRIVATE_KEY']) {
                            sh """
                                ssh -t -o StrictHostKeyChecking=no root@47.254.202.80 'docker login --username=AWS --password=${password} 634937900606.dkr.ecr.ap-east-1.amazonaws.com && docker pull 634937900606.dkr.ecr.ap-east-1.amazonaws.com/ga-vendor-api-service:qa'
                            """
                            sh 'ssh -t -o StrictHostKeyChecking=no root@47.254.202.80 docker service update --force --image 634937900606.dkr.ecr.ap-east-1.amazonaws.com/ga-vendor-api-service:qa game-aggregator_ga-vendor-api-service'
                        }
                    }
                }
            }
        }

        stage('Deploy in ECS') {
            when {
                not {
                    branch 'qa'
                }
            }
            // Use the Amazon AWS CLI Docker image as the build agent
            agent {
                docker {
                    image 'amazon/aws-cli'
                    args '--entrypoint=""'
                }
            }
            steps {
                // Update a task definition with a new Docker image and deploy it to Amazon ECS
                withAWS(region: "${AWS_ECS_REGION}", credentials: "${JENKINS_CREDENTIALS}") {
                    script {
                        String packageVersion = getRepoTag(env.BRANCH_NAME)

                        updateContainerDefinitionJsonWithImageVersion(packageVersion)
                        sh("aws ecs register-task-definition --region ${AWS_ECS_REGION} --family ${AWS_ECS_TASK_DEFINITION} --execution-role-arn ${AWS_ECS_EXECUTION_ROL} --requires-compatibilities ${AWS_ECS_COMPATIBILITY} --network-mode ${AWS_ECS_NETWORK_MODE} --cpu ${AWS_ECS_CPU} --memory ${AWS_ECS_MEMORY} --container-definitions file://${AWS_ECS_TASK_DEFINITION_PATH}")
                        String taskRevision = sh(script: "aws ecs describe-task-definition --task-definition ${AWS_ECS_TASK_DEFINITION} | grep -oP '\"revision\": \\K\\d+'", returnStdout: true)
                        sh("aws ecs update-service --cluster ${AWS_ECS_CLUSTER} --service ${AWS_ECS_SERVICE} --task-definition ${AWS_ECS_TASK_DEFINITION}:${taskRevision}")
                    }
                }
            }
        }
    }

    post {
        always {
            discordSend description: "${currentBuild.currentResult}: ${env.JOB_NAME} #${currentBuild.number}", title: 'Pipeline Status', webhookURL: 'https://discord.com/api/webhooks/1055669297151746049/6hhQcW2n2z5FfiDCzKNioMDV7bMm10HyaSebl4CqqDUXpbSU2L9R5-HoVuNu7sL9NIsl', link: 'http://223.25.67.48:8080/job/Game%20Aggregator/job/Develops/job/Vendor%20API%20Service/'
        }
    }
}

// A function to update the container definition JSON file with the new Docker image version
void updateContainerDefinitionJsonWithImageVersion(String packageVersion) {
    List containerDefinitionJson = readJSON file: AWS_ECS_TASK_DEFINITION_PATH, returnPojo: true
    containerDefinitionJson[0]['image'] = "${AWS_ECR_URL}:${packageVersion}".inspect()
    echo "task definition JSON: ${containerDefinitionJson}"
    writeJSON file: AWS_ECS_TASK_DEFINITION_PATH, json: containerDefinitionJson
}

String getRepoTag(String branchName) {
    String packageVersion = 'dev'

    switch (branchName) {
        case 'staging':
            packageVersion = 'stg'
            break
        case 'qa':
            packageVersion = 'qa'
            break
        case 'pt':
            packageVersion = 'pt'
            break
        case 'devops':
            packageVersion = 'pt'
            break
    }

    // String packageVersion = sh(script: "git describe --tags --always --dirty", returnStdout: true)
    return packageVersion
}

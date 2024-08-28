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

                    String branchName = BRANCH_NAME.tokenize('/')[0]
                    String configName = "${branchName}_config"

                    configFileProvider([configFile(fileId: configName, variable: 'CONFIG_FILE')]) {
                        def props = readProperties file: CONFIG_FILE

                        props.each { key, value ->
                            env."${key}" = value.trim()
                        }
                    }
                }
            }
        }

        stage('Versioning') {
            when {
                environment name: 'TRIGGER_VERSIONING', value: 'true'
            }

            steps {
                script {
                    withCredentials([gitUsernamePassword(credentialsId: "${GIT_CREDENTIALS_ID}", gitToolName: 'Default')]) {
                        String branchName = BRANCH_NAME.tokenize('/')[0]
                        String versionTag
                        String releaseFile = 'release.txt'

                        if (branchName == 'release' && sh(returnStatus: true, script: "test ! -f ${releaseFile}") == 0) {
                            versionTag = getVersionTagbyBranch(BRANCH_NAME)
                            sh "mvn versions:set -DnewVersion=$versionTag"

                            sh """
                                git add .
                                git commit -m "Update version to ${versionTag}"
                                git push origin HEAD:refs/heads/${BRANCH_NAME}
                                echo '${versionTag}' > ${releaseFile}
                            """
                        }

                        versionTag = getVersionTagbyPom(branchName)
                        sh "mvn versions:set -DnewVersion=$versionTag"
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
                        String branchName = BRANCH_NAME.tokenize('/')[0]
                        String versionTag = getVersionTagbyPom(branchName)

                        sh """
                            git tag -a ${versionTag} -m "${commitMessage}"
                            git push origin ${versionTag}
                        """
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

        stage('Build Project') {
            steps {
                script {
                    withCredentials([file(credentialsId: "${COUCHBASE_CREDENTIALS_ID}", variable: 'SECRET_FILE')]) {
                        sh 'cp -rf $SECRET_FILE ./game_aggregator-root-certificate.pem'
                    }

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
                script {
                    withAWS(region: "${AWS_ECR_REGION}", credentials: "${AWS_CREDENTIALS_ID}") {
                        String branchName = BRANCH_NAME.tokenize('/')[0]
                        String branchTag = BRANCH_TAG
                        String dockerTag = getVersionTagbyPom(branchName)

                        sh("#!/bin/sh -e\n${ecrLogin()}")
                        docker.image("${AWS_ECR_URL}:${branchTag}").push("${branchTag}")
                        docker.image("${AWS_ECR_URL}:${branchTag}").push("${dockerTag}")
                    }
                }
            }
        }

        stage('Deploy to EC2') {
            when {
                environment name: 'TRIGGER_DEPLOY_EC2', value: 'true'
            }
            steps {
                withAWS(region: "${AWS_ECR_REGION}", credentials: "${AWS_CREDENTIALS_ID}") {
                    script {
                        String login = ecrLogin()
                        String branchTag = BRANCH_TAG
                        String branchName = BRANCH_NAME.tokenize('/')[0]
                        String envAddOptions

                        configFileProvider([configFile(fileId: "${branchName}_env", variable: 'propertiesFilePath')]) {
                            envAddOptions = generateDockerEnvAddOptions(propertiesFilePath)
                        }

                        sshagent(credentials: ["${SERVER_SSH_CREDENTIALS_ID}"]) {
                            sh "#!/bin/sh -e\nssh -t -o StrictHostKeyChecking=no ${SERVER_SSH} '${login}'"
                            sh "ssh -t -o StrictHostKeyChecking=no ${SERVER_SSH} \"docker pull ${AWS_ECR_URL}:${branchTag} && docker service update --force ${envAddOptions} --image ${AWS_ECR_URL}:${branchTag} --update-order start-first --update-delay 30s ${PORTAINER_SERVICE_NAME}\""
                        }
                    }
                }
            }
        }

        stage('Deploy to ECS') {
            when {
                environment name: 'TRIGGER_DEPLOY_ECS', value: 'true'
            }
            steps {
                script {
                    withAWS(region: "${AWS_ECS_REGION}", credentials: "${AWS_CREDENTIALS_ID}") {
                        script {
                            String branchName = BRANCH_NAME.tokenize('/')[0]
                            String branchTag = BRANCH_TAG

                            configFileProvider([configFile(fileId: "${branchName}_td", variable: 'taskDefinitionPath')]) {
                                configFileProvider([configFile(fileId: "${branchName}_env", variable: 'propertiesFilePath')]) {
                                    updateContainerDefinitionJsonWithImageVersion(branchTag, taskDefinitionPath)
                                    updateContainerDefinitionJsonWithEnvVars(propertiesFilePath, taskDefinitionPath)
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
        }
    }

    post {
        always {
            script {
                if (TRIGGER_DISCORD == 'true') {
                    discordSend description: "${currentBuild.currentResult}: ${JOB_NAME} #${currentBuild.number}", title: "Pipeline ${currentBuild.fullProjectName} ${currentBuild.currentResult}", webhookURL: DISCORD_WEBHOOK_URL, link: currentBuild.absoluteUrl, result: currentBuild.currentResult, showChangeset: true
                }
            }
        }
    }
}

String getVersionTagbyPom(String branchName) {
    String baseVersion
    String mavenVersion = readMavenPom().version.trim()
    String normalizedBranch = branchName
    def matcher = mavenVersion =~ /(\d+\.\d+\.\d+(?:-(?:hotfix|adhoc))?)/

    if (matcher.find()) {
        baseVersion = matcher.group(1)
    }

    if (branchName == 'release') {
        normalizedBranch = 'rc'
    }

    return (branchName == 'main') ? "${baseVersion}" : "${baseVersion}-${normalizedBranch}.${BUILD_NUMBER}"
}

String getVersionTagbyBranch(String branchName) {
    String baseVersion

    def matcher = branchName =~ /release\/(\d+\.\d+\.\d+)/

    if (matcher.find()) {
        baseVersion = matcher.group(1)
    }

    return "${baseVersion}"
}

void updateContainerDefinitionJsonWithImageVersion(String branchTag, String taskDefinitionPath) {
    List containerDefinitionJson = readJSON file: taskDefinitionPath, returnPojo: true
    containerDefinitionJson[0]['image'] = "${AWS_ECR_URL}:${branchTag}".inspect()
    writeJSON file: taskDefinitionPath, json: containerDefinitionJson
}

void updateContainerDefinitionJsonWithEnvVars(String propertiesFilePath, String taskDefinitionPath) {
    def props = readProperties file: propertiesFilePath
    def envVars = props.collect { key, value ->
        def envVarName = key.toUpperCase().replace('.', '_')
        [name: envVarName, value: value]
    }

    List containerDefinitionJson = readJSON file: taskDefinitionPath, returnPojo: true
    containerDefinitionJson[0]['environment'] = envVars
    writeJSON file: taskDefinitionPath, json: containerDefinitionJson
}

String generateDockerEnvAddOptions(String propertiesFilePath) {
    def props = readProperties file: propertiesFilePath
    def escapeShellArg = { value ->
        return value.replace("'", "\\'")
                    .replace('"', '\\"')
                    // .replace('&', '\\&')
    }

    def envOptions = props.collect { key, value ->
        def envVarName = key.toUpperCase().replace('.', '_')
        def escapedValue = escapeShellArg(value)
        return "--env-add '${envVarName}=${escapedValue}'"
    }.join(' ')

    return envOptions
}

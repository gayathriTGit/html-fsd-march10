def scan_type
def target 
pipeline{

    agent any 

    parameters {
        choice  choices: ['Baseline', 'APIS', 'Full'],
                 description: 'Type of scan that is going to perform inside the container',
                 name: 'SCAN_TYPE'

        string defaultValue: 'https://medium.com/',  // URL to scan
                 description: 'Target URL to scan',
                 name: 'TARGET'

    }

    stages{
        stage('Setting up of the OWASP ZAP container'){
    
            steps{
                echo "pulling Image of ZAP ---> Start"
                sh 'docker pull ghcr.io/zaproxy/zaproxy:stable'
                echo "pulling of Image completed ---> End"
                echo "Running Image ---> Starting Container"
                sh 'docker run -dt --name owasp-$BUILD_NUMBER ghcr.io/zaproxy/zaproxy:stable'
            }
        }

        stage('Creating a working Directory in the container'){

            steps{
                echo "Connect to container and execute command to create a directory"
                sh 'docker exec owasp-$BUILD_NUMBER mkdir /zap/wrk'
            }
        }

        stage('Scan target on owasp container') {
            steps {
                script {
			        scan_type = "${params.SCAN_TYPE}"
                    echo "----> scan_type: $scan_type"
                    target = "${params.TARGET}"

                    if (scan_type == 'Baseline') {
                        sh """
                             docker exec owasp-$BUILD_NUMBER \
                             zap-baseline.py \
                             -t $target \
                             -r report.html \
                             -I
                         """
                    }
                    else if (scan_type == 'APIS') {
                        sh """
                             docker exec owasp-$BUILD_NUMBER \
                             zap-api-scan.py \
                             -t $target \
                             -r report.html \
                             -I
                         """
                     }
                     else if (scan_type == 'Full') {
                        sh """
                             docker exec owasp-$BUILD_NUMBER \
                             zap-full-scan.py \
                             -t $target \
                             -r report.html \
                             -I
                         """
                     }
                     else {
                        echo 'Something went wrong...'
                     }
                }
            }
        }

        stage('Copy Report to Workspace') {
            steps {
                script {
                    sh '''
                         docker cp owasp-$BUILD_NUMBER:/zap/wrk/report.html ${WORKSPACE}/report.html
                     '''
                }
            }
        }
    }

    post {
        always {
            echo 'Removing container'
            sh '''
                     docker stop owasp-$BUILD_NUMBER
                     docker rm owasp-$BUILD_NUMBER
                '''
        }

    }

}
        
        
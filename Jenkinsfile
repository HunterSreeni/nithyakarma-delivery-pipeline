pipeline {
    agent any

    options {
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()      // one run at a time: shared test accounts, one Job name
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        choice(name: 'SUITE', choices: ['testng-regression.xml', 'testng-dag.xml'], description: 'TestNG suite')
        booleanParam(name: 'E2E_ALLOW_DATA_WRITES', defaultValue: false, description: 'Run write tests T8, T9, T12 (production test accounts)')
    }

    environment {
        IMAGE      = "nk-runner:${env.BUILD_NUMBER}"
        KUBECONFIG = credentials('nk-kubeconfig')   // file credential -> path to a temp copy
        NS         = 'nk-e2e'
        // a new job learns its parameters only during build #1, so params are empty then: fall back to the safe defaults
        RUN_SUITE  = "${params.SUITE ?: 'testng-regression.xml'}"
        RUN_WRITES = "${params.E2E_ALLOW_DATA_WRITES ?: false}"
    }

    stages {
        stage('Build runner image') {
            steps {
                sh 'docker build -t "$IMAGE" .'     // also compiles the tests (RUN mvn test-compile)
                sh 'kind load docker-image "$IMAGE" --name nk'
            }
        }

        stage('Secret + run settings') {
            steps {
                withCredentials([
                    string(credentialsId: 'nk-base-url',         variable: 'BASE_URL'),
                    string(credentialsId: 'nk-supabase-url',     variable: 'SUPABASE_URL'),
                    string(credentialsId: 'nk-publishable-key',  variable: 'SUPABASE_PUBLISHABLE_KEY'),
                    string(credentialsId: 'nk-service-role-key', variable: 'SUPABASE_SERVICE_ROLE_KEY'),
                    string(credentialsId: 'nk-user-a-email',     variable: 'E2E_USER_A_EMAIL'),
                    string(credentialsId: 'nk-user-b-email',     variable: 'E2E_USER_B_EMAIL')
                ]) {
                    // echo is a shell builtin, so secret values never show up in the process list;
                    // the temp file lives outside the workspace and is removed even on failure
                    sh '''
                        mkdir -p "$WORKSPACE_TMP"
                        trap 'rm -f "$WORKSPACE_TMP/nk.env"' EXIT
                        umask 077
                        {
                          echo "BASE_URL=$BASE_URL"
                          echo "SUPABASE_URL=$SUPABASE_URL"
                          echo "SUPABASE_PUBLISHABLE_KEY=$SUPABASE_PUBLISHABLE_KEY"
                          echo "SUPABASE_SERVICE_ROLE_KEY=$SUPABASE_SERVICE_ROLE_KEY"
                          echo "E2E_USER_A_EMAIL=$E2E_USER_A_EMAIL"
                          echo "E2E_USER_B_EMAIL=$E2E_USER_B_EMAIL"
                        } > "$WORKSPACE_TMP/nk.env"
                        kubectl -n "$NS" create secret generic nk-env --from-env-file="$WORKSPACE_TMP/nk.env" --dry-run=client -o yaml | kubectl apply -f -
                    '''
                }
                // run settings are not secrets: they go in a ConfigMap
                sh 'kubectl -n "$NS" create configmap nk-run --from-literal=SUITE="$RUN_SUITE" --from-literal=E2E_ALLOW_DATA_WRITES="$RUN_WRITES" --dry-run=client -o yaml | kubectl apply -f -'
            }
        }

        stage('Run tests in kind') {
            steps {
                // kubectl wait takes one condition and hangs on a failed Job, so wait for Complete OR Failed
                sh '''
                    kubectl -n "$NS" delete job nk-e2e --ignore-not-found --wait=true
                    docker exec nk-control-plane rm -rf /reports/surefire /reports/console
                    sed "s|nk-runner:0.1.0|$IMAGE|" k8s/e2e-job.yml | kubectl apply -f -
                    kubectl -n "$NS" wait --for=condition=Ready pod -l job-name=nk-e2e --timeout=5m
                    kubectl -n "$NS" logs -f job/nk-e2e -c runner
                    until kubectl -n "$NS" get job nk-e2e -o jsonpath='{.status.conditions[*].type}' | grep -qE 'Complete|Failed'; do sleep 2; done
                    test "$(kubectl -n "$NS" get job nk-e2e -o jsonpath='{.status.succeeded}')" = "1"
                '''
            }
        }
    }

    post {
        always {
            // collect reports FIRST, delete the Job LAST.
            // Jenkins cannot see the D:\ drive, so copy them out of the kind node.
            sh '''
                rm -rf job-reports && mkdir job-reports
                docker cp nk-control-plane:/reports/surefire job-reports/ || true
                docker cp nk-control-plane:/reports/console job-reports/ || true
            '''
            junit allowEmptyResults: true, testResults: 'job-reports/surefire/TEST-*.xml'
            archiveArtifacts artifacts: 'job-reports/**', allowEmptyArchive: true
            sh 'kubectl -n "$NS" delete job nk-e2e --ignore-not-found'
        }
    }
}

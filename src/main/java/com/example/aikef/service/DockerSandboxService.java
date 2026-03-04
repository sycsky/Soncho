package com.example.aikef.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DockerSandboxService {

    private DockerClient dockerClient;

    @Value("${docker.host:tcp://localhost:2375}")
    private String dockerHost;

    @Value("${docker.enabled:false}")
    private boolean enabled;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Docker Sandbox is disabled.");
            return;
        }

        try {
            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .build();

            ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                    .dockerHost(config.getDockerHost())
                    .sslConfig(config.getSSLConfig())
                    .maxConnections(100)
                    .build();

            this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
            
            log.info("Docker Client initialized successfully.");
        } catch (Exception e) {
            log.error("Failed to initialize Docker Client", e);
            // Don't disable here, maybe transient. Or yes, disable to avoid future errors.
            // this.enabled = false; 
        }
    }

    /**
     * Executes Python code in an isolated Docker container.
     *
     * @param pythonCode The Python code to execute.
     * @param inputJson  Input JSON string (passed as stdin or argument).
     * @return The execution output (stdout/stderr).
     */
    public String executePython(String pythonCode, String inputJson) {
        if (!enabled || dockerClient == null) {
            return "Error: Docker Sandbox is not enabled or initialized.";
        }

        String containerId = null;
        try {
            // Prepare script: We wrap the user code to handle input and print output
            // We assume the user code has `def lambda_handler(event, context):`
            // We need to escape single quotes in pythonCode and inputJson to prevent syntax errors in wrapper
            // A better way is to pass code as file via bind mount or stdin, but for simplicity we inject.
            // CAUTION: Injection vulnerability if not properly escaped.
            // Let's use a very simple escaping for POC.
            
            // To be safer, we should write the code to a temporary file on host and bind mount it.
            // But that requires managing temp files.
            
            // Let's try passing the code as an environment variable or just run it via `python -c`.
            // The wrapper script approach is fragile with string formatting.
            
            // Revised Approach:
            // 1. Create container with `cat > script.py` (via sh -c)
            // 2. Or just use a very simple wrapper that imports sys, json.
            
            // For this POC, let's assume the input code is simple and we escape basic chars.
            String safeCode = pythonCode.replace("\"", "\\\"").replace("'", "\\'");
            String safeInput = inputJson != null ? inputJson.replace("\"", "\\\"") : "{}";

            String cmd = "python -c \"import json; import sys; " +
                    "def lambda_handler(event, context): " +
                    "    " + safeCode + "; " +  // This is tricky because python relies on indentation.
                    "print(json.dumps(lambda_handler(json.loads('" + safeInput + "'), {})))\"";
            
            // Wait, Python indentation via one-liner is hard.
            // Better: Write code to a file inside container?
            // `echo "code" > script.py`
            
            // Let's go with the `sh -c` trick to write file.
            // Or better: `docker cp` (copyArchiveToContainerCmd).
            
            // Step 1: Create Container (Command: tail -f /dev/null to keep running)
            CreateContainerResponse container = dockerClient.createContainerCmd("python:3.9-slim")
                    .withCmd("tail", "-f", "/dev/null") 
                    .withHostConfig(HostConfig.newHostConfig()
                            .withAutoRemove(true)
                            .withMemory(128 * 1024 * 1024L)
                            .withNanoCPUs(500000000L))
                    .withNetworkDisabled(true)
                    .exec();
            
            containerId = container.getId();
            dockerClient.startContainerCmd(containerId).exec();
            
            // Step 2: Write script to container using sh -c
            // Use Base64 encoding to avoid escaping hell
            String scriptContent = String.format("""
import json
import sys

# User Code
%s

# Harness
if __name__ == "__main__":
    try:
        input_str = '%s'
        event = json.loads(input_str)
        context = {}
        result = lambda_handler(event, context)
        print(json.dumps(result))
    except Exception as e:
        print(f"Error: {str(e)}", file=sys.stderr)
""", pythonCode, inputJson != null ? inputJson.replace("'", "\\'") : "{}");

            String base64Script = java.util.Base64.getEncoder().encodeToString(scriptContent.getBytes());

            // Write to file via base64 decode
            // Note: Alpine linux uses `base64 -d`, but standard Debian/Ubuntu (python:3.9-slim) uses `base64 -d` too from coreutils.
            String execIdWrite = dockerClient.execCreateCmd(containerId)
                    .withCmd("sh", "-c", "echo " + base64Script + " | base64 -d > /app.py")
                    .exec()
                    .getId();
            
            dockerClient.execStartCmd(execIdWrite).exec(new ResultCallback.Adapter<Frame>() {
                @Override
                public void onNext(Frame frame) {
                    // ignore
                }
            }).awaitCompletion(5, TimeUnit.SECONDS);

            // Step 3: Execute python /app.py
            // We use execCreateCmd again to run the script and capture output
            String execId = dockerClient.execCreateCmd(containerId)
                    .withCmd("python", "/app.py")
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .exec()
                    .getId();

            StringBuilder output = new StringBuilder();
            dockerClient.execStartCmd(execId)
                    .exec(new ResultCallback.Adapter<Frame>() {
                        @Override
                        public void onNext(Frame frame) {
                            output.append(new String(frame.getPayload()));
                        }
                    }).awaitCompletion(10, TimeUnit.SECONDS);

            return output.toString().trim();

        } catch (Exception e) {
            log.error("Docker execution failed", e);
            return "Error: Sandbox execution failed - " + e.getMessage();
        } finally {
             if (containerId != null) {
                try {
                     dockerClient.stopContainerCmd(containerId).exec(); // AutoRemove should trigger
                } catch (Exception ignored) {}
            }
        }
    }
}

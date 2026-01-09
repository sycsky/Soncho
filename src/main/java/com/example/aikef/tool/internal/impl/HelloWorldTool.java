package com.example.aikef.tool.internal.impl;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class HelloWorldTool {

    @Tool("A simple Hello World tool that returns a greeting message")
    public String helloWorld(@P(value = "The name of the person", required = true) String name) {
        return "Hello, " + name + "! Welcome to the internal tool world.";
    }
}

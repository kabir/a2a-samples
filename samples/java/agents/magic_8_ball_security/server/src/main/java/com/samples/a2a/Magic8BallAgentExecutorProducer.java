package com.samples.a2a;

import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.Task;
import org.a2aproject.sdk.spec.TaskNotCancelableError;
import org.a2aproject.sdk.spec.TaskState;
import org.a2aproject.sdk.spec.TextPart;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

/**
 * Producer for Magic 8 Ball agent executor.
 */
@ApplicationScoped
public final class Magic8BallAgentExecutorProducer {

    /**
     * The Magic 8 Ball agent instance.
     */
    @Inject
    private Magic8BallAgent magic8BallAgent;

    /**
     * Produces the agent executor for the Magic 8 Ball agent.
     *
     * @return the configured agent executor
     */
    @Produces
    public AgentExecutor agentExecutor() {
        return new Magic8BallAgentExecutor(magic8BallAgent);
    }

    /**
     * Magic 8 Ball agent executor implementation.
     */
    private static class Magic8BallAgentExecutor implements AgentExecutor {

        /**
         * The Magic 8 Ball agent instance.
         */
        private final Magic8BallAgent agent;

        /**
         * Constructor for Magic8BallAgentExecutor.
         *
         * @param magic8BallAgentInstance the Magic 8 Ball agent instance
         */
        Magic8BallAgentExecutor(final Magic8BallAgent magic8BallAgentInstance) {
            this.agent = magic8BallAgentInstance;
        }

        @Override
        public void execute(final RequestContext context,
                            final AgentEmitter emitter)
                throws A2AError {

            // mark the task as submitted and start working on it
            if (context.getTask() == null) {
                emitter.submit();
            }
            emitter.startWork();

            // extract the text from the message
            final String question = context.getUserInput();

            // Generate a unique memory ID for this request for fresh chat memory
            final String memoryId = UUID.randomUUID().toString();
            System.out.println(
                    "=== EXECUTOR === Using memory ID: "
                            + memoryId + " for question: " + question);

            // call the Magic 8 Ball agent with the question
            final String response;
            if (Boolean.getBoolean("skip.agent")) {
                response = new Magic8BallTools().shakeMagic8Ball(question);
            } else {
                 response = agent.answerQuestion(memoryId, question);
            }

            // add the response as an artifact and complete the task
            emitter.addArtifact(List.of(new TextPart(response, null)));
            emitter.complete();
        }

        @Override
        public void cancel(final RequestContext context,
                           final AgentEmitter emitter)
                throws A2AError {
            final Task task = context.getTask();

            if (task.status().state() == TaskState.TASK_STATE_CANCELED) {
                throw new TaskNotCancelableError();
            }

            if (task.status().state() == TaskState.TASK_STATE_COMPLETED) {
                throw new TaskNotCancelableError();
            }

            emitter.cancel();
        }
    }
}

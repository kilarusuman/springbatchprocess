package com.tm.demo.springboot.tasklet;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import com.tm.demo.springboot.config.JobConfig;
public class MoveFilesTasklet implements Tasklet {
	
	Logger logger = LoggerFactory.getLogger(MoveFilesTasklet.class);
	
	private String resourcesPath;
	
	public String getResourcesPath() {
		return resourcesPath;
	}

	public void setResourcesPath(String resourcesPath) {
		this.resourcesPath = resourcesPath;
	}

	private Resource[] resources;

	@Override
	public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
		Collection<StepExecution> stepExecutions = chunkContext.getStepContext().getStepExecution().getJobExecution()
				.getStepExecutions();
		for (StepExecution stepExecution : stepExecutions) {
			if (stepExecution.getExecutionContext().containsKey("fileName")
					&& ExitStatus.COMPLETED.equals(stepExecution.getExitStatus())
					&& stepExecution.getFailureExceptions().size() <= 0) {
				String file = stepExecution.getExecutionContext().getString("fileName");
				String path = file.replace("file:/", "");
				String[] filename = file.split("/");
				FileUtils.moveFile(FileUtils.getFile(path),
						FileUtils.getFile(resourcesPath + filename[4]));
				FileUtils.deleteQuietly(FileUtils.getFile(path));

			}
		}

		return RepeatStatus.FINISHED;

	}

	public void setResources(Resource[] resources) {
		this.resources = resources;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(resources, "directory must be set");
	}

}

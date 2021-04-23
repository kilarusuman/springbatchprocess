/**
 * 
 */
package com.tm.demo.springboot.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.partition.support.MultiResourcePartitioner;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.tm.demo.springboot.listener.VechicleJobListener;
import com.tm.demo.springboot.listener.VechicleStepListener;
import com.tm.demo.springboot.model.Vehicle;
import com.tm.demo.springboot.processor.VehicleItemProcessor;
import com.tm.demo.springboot.tasklet.FileZipandDeletingTasklet;
import com.tm.demo.springboot.tasklet.MoveErrorFilesTasklet;
import com.tm.demo.springboot.tasklet.MoveFilesTasklet;
import com.tm.demo.springboot.util.Constants;
import com.tm.demo.springboot.util.VehiclePreparedSmtSetter;

/**
 * @author Damodhara Palavali
 *
 */
@Configuration
@EnableBatchProcessing
@EnableScheduling
public class JobConfig {
	Logger logger = LoggerFactory.getLogger(JobConfig.class);

	@Value("${files.path}")
	private String resourcesPath;
	@Value("${files.error.path}")
	private String errorPath;
	@Value("${files.success.path}")
	private String sucessPath;
	@Value("${files.zip.path}")
	private String zipPath;
	@Value("${files.type}")
	private String fileType;
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	@Autowired
	private JobLauncher jobLauncher;
	@Autowired
	private DataSource dataSource;

	@Scheduled(cron = "${spring.batch.job.cron.expression}")
	public void fileProcessSchedule() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addDate("launchDate", new Date())
					.toJobParameters();
			jobLauncher.run(fileProcessJob(), jobParameters);
		} catch (Exception e) {
			logger.error("Exception in fileProcessSchedule ",e.getMessage());
		}
	}

	@Scheduled(cron = "${spring.batch.job.cron.zip.expression}")
	public void fileZIPSchedule() {
		try {
			JobParameters jobParameters = new JobParametersBuilder().addDate("launchDate", new Date())
					.toJobParameters();
			jobLauncher.run(zipfilesJob(), jobParameters);
		} catch (Exception e) {
			logger.error("Exception in fileZIPSchedule ",e.getMessage());
		}
	}

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setMaxPoolSize(5);
		taskExecutor.setCorePoolSize(5);
		taskExecutor.afterPropertiesSet();
		return taskExecutor;
	}

	public Job zipfilesJob() throws Exception {
		return jobBuilderFactory.get("zipfilesJob").incrementer(new RunIdIncrementer()).flow(deleteAndZipFiles()).next(deleteAndZipErrorFiles()).end()
				.build();
	}

	@Bean
	public Step deleteAndZipFiles() {
		FileZipandDeletingTasklet zipSucessFilesTasklet = new FileZipandDeletingTasklet();
			zipSucessFilesTasklet.setResourcesPath(sucessPath);
			zipSucessFilesTasklet.setZipPath(zipPath);

			return stepBuilderFactory.get("deleteAndZipFiles").tasklet(zipSucessFilesTasklet).build();
	}
	@Bean
	public Step deleteAndZipErrorFiles() {
		FileZipandDeletingTasklet zipErrorFilesTasklet = new FileZipandDeletingTasklet();
			zipErrorFilesTasklet.setResourcesPath(errorPath);
			zipErrorFilesTasklet.setZipPath(zipPath);
			return stepBuilderFactory.get("deleteAndZipErrorFiles").tasklet(zipErrorFilesTasklet).build();
	}
	@Bean
	public Job fileProcessJob() throws Exception {
		return jobBuilderFactory.get("fileProcessJob").incrementer(new RunIdIncrementer())
				.listener(new VechicleJobListener()).start(masterStep()).on("COMPLETED").to(moveFiles())
				.from(masterStep()).on("UNKNOWN").to(moveErrorFiles()).end().build();
	}

	@Bean
	public Step masterStep() throws Exception {
		return stepBuilderFactory.get("masterStep").partitioner(slaveStep()).partitioner("partition", partitioner())
				.taskExecutor(taskExecutor()).listener(new VechicleStepListener()).build();
	}

	@Bean
	public Step slaveStep() throws Exception {
		return stepBuilderFactory.get("slaveStep").<Vehicle, Vehicle>chunk(1)
				.reader(reader(null)).processor(processor(null, null)).writer(dbWriter()).build();
	}

	@Bean
	protected Step moveFiles() {
		MoveFilesTasklet moveFilesTasklet = new MoveFilesTasklet();
		try {
			moveFilesTasklet.setResourcesPath(sucessPath);
			moveFilesTasklet.setResources(new PathMatchingResourcePatternResolver().getResources("file:" + sucessPath + fileType));
		} catch (IOException e) {
			
		}
		return stepBuilderFactory.get("moveFiles").tasklet(moveFilesTasklet).build();
	}

	@Bean
	protected Step moveErrorFiles() {
		MoveErrorFilesTasklet moveFilesTasklet = new MoveErrorFilesTasklet();
		try {
			moveFilesTasklet.setResourcesPath(errorPath);
			moveFilesTasklet.setResources(new PathMatchingResourcePatternResolver().getResources("file:" + errorPath + fileType));
		} catch (IOException e) {
			
		}
		return stepBuilderFactory.get("moveErrorFiles").tasklet(moveFilesTasklet).build();
	}

	@Bean
	@JobScope
	public Partitioner partitioner() throws Exception {
		MultiResourcePartitioner partitioner = new MultiResourcePartitioner();
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		partitioner.setResources(resolver.getResources("file:" + resourcesPath + fileType));
		partitioner.partition(20);
		logger.info("--------partitioner()---end -No of files--->"
				+ resolver.getResources("file:" + resourcesPath + fileType).length);
		return partitioner;
	}

	 @Bean
	 @StepScope
     public FlatFileItemReader<Vehicle> reader(@Value("#{stepExecutionContext['fileName']}") String file) throws MalformedURLException {
        FlatFileItemReader<Vehicle> itemReader = new FlatFileItemReader<Vehicle>();
        itemReader.setLineMapper(lineMapper());
        itemReader.setLinesToSkip(1);
        itemReader.setResource(new UrlResource(file));
        return itemReader;
     }
	 
     @Bean
     public LineMapper<Vehicle> lineMapper() {
        DefaultLineMapper<Vehicle> lineMapper = new DefaultLineMapper<Vehicle>();
        DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
        lineTokenizer.setNames(new String[] { "vehiclenumber", "brand", "country","modelname", "modelyear" });
        lineTokenizer.setIncludedFields(new int[] { 0, 1, 2, 3, 4 });
        BeanWrapperFieldSetMapper<Vehicle> fieldSetMapper = new BeanWrapperFieldSetMapper<Vehicle>();
        fieldSetMapper.setTargetType(Vehicle.class);
        lineMapper.setLineTokenizer(lineTokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        return lineMapper;
     }
	
	@Bean
	@StepScope
	public VehicleItemProcessor processor(@Value("#{stepExecutionContext['fileName']}") String file,
			@Value("#{stepExecution.jobExecution.id}") String jobID) {
		String fileName = file.substring(file.lastIndexOf("/") + 1);
		VehicleItemProcessor factoryFeeditemProcessor = new VehicleItemProcessor();
		logger.debug("----processor----fileName--->" + file);
		factoryFeeditemProcessor.setProcessingFileName(fileName);
		factoryFeeditemProcessor.setProcessingJobid(jobID);
		return factoryFeeditemProcessor;
	}
	
	@Bean
	public JdbcBatchItemWriter<Vehicle> dbWriter() {
		JdbcBatchItemWriter<Vehicle> writer = new JdbcBatchItemWriter<>();
		writer.setDataSource(dataSource);
		writer.setSql(Constants.INSERT_QUERY_OLD);
		writer.setItemPreparedStatementSetter(new VehiclePreparedSmtSetter());
		return writer;
	}
	
}

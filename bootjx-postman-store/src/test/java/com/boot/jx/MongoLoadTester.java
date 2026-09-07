package com.boot.jx;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.data.annotation.Id;

import com.boot.jx.mongo.CommonMongoSourceProvider;
import com.boot.jx.mongo.MongoTemplateCommonImpl;
import com.boot.utils.ArgUtil;

public class MongoLoadTester {
	private static final Random PRNG = new Random();
	private static AtomicInteger ERRORS = new AtomicInteger(0);
	private static AtomicInteger TOTAL = new AtomicInteger(0);

	private static MongoTemplateCommonImpl initMongo(String tnt) {
		String mongoConnectionString = ArgUtil.parseAsString(System.getenv("mongo.connection.string"),
				"mongodb://mhmongoadmin:xxxxxxxx@mongo.mehery.io:27017");
		AppContextUtil.setTenant(tnt);
		String connectionString = mongoConnectionString + "/" + "meheryqa?authSource=admin&authMechanism=SCRAM-SHA-1"
				+ "&connectTimeoutMS=300000&minPoolSize=0&maxPoolSize=2&maxIdleTimeMS=900000&waitQueueMultiple=100";

		CommonMongoSourceProvider commonMongoSource = new CommonMongoSourceProvider();
		commonMongoSource.setDataSourceUrl(connectionString);
		commonMongoSource.setGlobalDataSourceUrl(connectionString);
		commonMongoSource.setGlobalDBProfix("tnt");

		MongoTemplateCommonImpl mongoTemplate = new MongoTemplateCommonImpl(
				commonMongoSource.getSource().getMongoDbFactory());
		mongoTemplate.setMongoSourceProvider(commonMongoSource);
		return mongoTemplate;
	}

	private static class Result {
		private final int wait;

		public Result(int code) {
			this.wait = code;
		}
	}

	public static Result compute(CounterObject obj) throws InterruptedException {
		int wait = PRNG.nextInt(3000);

		try {
			obj.setResult("" + obj.getCounter());
			MongoTemplateCommonImpl mongoTemplate = initMongo(obj.getTnt());
			TestObject ob = new TestObject();
			ob.setTnt(AppContextUtil.getTenant());
			ob.setStamp(System.currentTimeMillis());
			ob.setCounter(obj.getCounter());
			ob.setTitle(obj.getTitle());
			mongoTemplate.save(ob, "TEST");
			Thread.sleep(wait);
			// ob = mongoTemplate.findById(ob, TestObject.class, "TEST");
		} catch (Exception e) {
			obj.setResult("X");
			System.out.println("===================  Exception for " + obj.getCounter() + "  " + e.getMessage());
			ERRORS.getAndIncrement();
		}
		TOTAL.getAndIncrement();
		return new Result(wait);
	}

	public static void main(String[] args) throws InterruptedException, ExecutionException {
		List<List<CounterObject>> objects = new ArrayList<List<CounterObject>>();
		int tenants = 10;
		int maxThreads = 5;
		for (int j = 0; j < tenants; j++) {
			List<CounterObject> ot = new ArrayList<CounterObject>();
			for (int i = 0; i < maxThreads; i++) {
				ot.add(new CounterObject().title("d_" + maxThreads).counter(i).tnt("0_test_" + j));
			}
			objects.add(ot);
		}

		List<Callable<Result>> tasks = new ArrayList<Callable<Result>>();
		for (final List<CounterObject> object : objects) {
			for (final CounterObject ibject : object) {
				Callable<Result> c = new Callable<Result>() {
					@Override
					public Result call() throws Exception {
						return compute(ibject);
					}
				};
				tasks.add(c);
			}
		}

		ExecutorService exec = Executors.newCachedThreadPool();
		// some other exectuors you could try to see the different behaviours
		// ExecutorService exec = Executors.newFixedThreadPool(3);
		// ExecutorService exec = Executors.newSingleThreadExecutor();
		try {
			long start = System.currentTimeMillis();
			List<Future<Result>> results = exec.invokeAll(tasks);
			int sum = 0;
			for (Future<Result> fr : results) {
				sum += fr.get().wait;
				System.out.println(String.format("Task waited %d ms", fr.get().wait));
			}
			long elapsed = System.currentTimeMillis() - start;
			System.out.println(String.format("Elapsed time: %d ms", elapsed));
			System.out.println(String.format("... but compute tasks waited for total of %d ms; speed-up of %.2fx", sum,
					sum / (elapsed * 1d)));

			System.out.println(String.format("ERRORS e: %d for %d", ERRORS.get(), maxThreads * tenants));

			for (int j = 0; j < tenants; j++) {
				List<CounterObject> ot = objects.get(j);
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < maxThreads; i++) {
					CounterObject o = ot.get(i);
					sb.append(" " + o.getResult());
				}
				System.out.println(sb.toString());
			}

		} finally {
			exec.shutdown();
		}
	}

	public static class CounterObject {
		String title;
		long counter;
		String result;

		public long getCounter() {
			return counter;
		}

		public void setCounter(long counter) {
			this.counter = counter;
		}

		public CounterObject counter(long counter) {
			this.counter = counter;
			return this;
		}

		String tnt;

		public String getTnt() {
			return tnt;
		}

		public void setTnt(String tnt) {
			this.tnt = tnt;
		}

		public CounterObject tnt(String tnt) {
			this.tnt = tnt;
			return this;
		}

		public CounterObject title(String title) {
			this.title = title;
			return this;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getResult() {
			return result;
		}

		public void setResult(String result) {
			this.result = result;
		}
	}

	public static class TestObject extends CounterObject {
		@Id
		String testId;
		long stamp;

		public String getTestId() {
			return testId;
		}

		public void setTestId(String testId) {
			this.testId = testId;
		}

		public long getStamp() {
			return stamp;
		}

		public void setStamp(long stamp) {
			this.stamp = stamp;
		}
	}
}

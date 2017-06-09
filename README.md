# PPE4J [![Build Status](https://travis-ci.org/ViktorC/PPE4J.svg?branch=master)](https://travis-ci.org/ViktorC/PPE4J) [![Quality Gate](https://sonarqube.com/api/badges/gate?key=net.viktorc:ppe4j)](https://sonarqube.com/dashboard/index/net.viktorc:ppe4j) [![Quality Gate](https://sonarqube.com/api/badges/measure?key=net.viktorc:ppe4j&metric=coverage)](https://sonarqube.com/dashboard/index/net.viktorc:ppe4j) [![Quality Gate](https://sonarqube.com/api/badges/measure?key=net.viktorc:ppe4j&metric=ncloc)](https://sonarqube.com/dashboard/index/net.viktorc:ppe4j)
__PPE4J__ (Process Pool Executor for Java) is a library that provides a pool for maintaining pre-started, interactive process shells. The pool communicates with the processes via their standard in and standard and error out streams. A [*ProcessPoolExecutor*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/ProcessPoolExecutor) can be created by constructing a [*StandardProcessPoolExecutor*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/StandardProcessPoolExecutor) instance directly or using the one of the methods of the [*ProcessPoolExecutors*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/ProcessPoolExecutors) class. The first parameter of both the constructor and the convenience methods is an implementation of the [*ProcessManagerFactory*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/ProcessManagerFactory) functional interface for creating new instances of an implementation of the [*ProcessManager*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/ProcessManager) interface. These instances are responsible for handling the start-up and possibly the termination of the processes kept in the pool. Other parameters of the *StandardProcessPoolExecutor* constructor include the minimum and maximum size of the pool, its reserve size, and the time interval after which idle processes in the pool should be terminated. The size of the pool is always kept between the minimum pool size and the maximum pool size (both inclusive). The reserve size specifies the minimum number of processes that should always be available. Once the process pool is initialized, it accepts commands in the form of implementations of the [*Submission*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/Submission) interface. Such an implementation contains one or more [*Command*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/Command) implementations and a boolean value that determines whether the process should be terminated after the execution of the submission. The submission is assigned to any one of the available processes in the pool. While executing a submission, the process cannot accept further submissions. The *Command* implementation allows for communication with a process via its standard in and standard/error out. This implementation specifies the instruction to send to the process' standard in and handles the output generated by the process as a response to the instruction. Moreover, the *Command* implementation also determines when the instruction may be considered processed and when the process is ready for the next instruction. The library also provides some basic implementations of the *ProcessManager*, *Submission*, and *Command* interfaces to allow for the concise definition of process pooling systems for typical situations. The complete Javadoc can be found [here](http://viktorc.github.io/PPE4J/).
## Sample code

	ProcessPoolExecutor pool = new StandardProcessPoolExecutor(() ->
			new AbstractProcessManager(new ProcessBuilder("test.exe")) {
		
		@Override
		public boolean startsUpInstantly() {
			// The process is not instantly ready for instructions.
			return false;
		}
		@Override
		public boolean isStartedUp(String output, boolean standard) {
			// Once it has output "hi", it is ready to read from its standard in.
			return !standard || "hi".equals(output);
		}
		@Override
		public void onStartup(ProcessShell shell) {
			/* Execute the command "start" on every process of the pool before it is 
			 * added to the list of available processes. Consider "start" processed 
			 * once the string "ok" has been output to the process' standard out. */
			try {
				shell.execute(new SimpleSubmission(new SimpleCommand("start",
						(c, o) -> "ok".equals(o), (c, o) -> true), false));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		@Override
		public boolean terminate(ProcessShell shell) {
			try {
				/* Attempt to exit the process in an orderly way, if it fails let it be 
				 * terminated forcibly. */
				AtomicBoolean success = new AtomicBoolean(true);
				if (shell.execute(new SimpleSubmission(new SimpleCommand("stop",
						/* If the string "bye" is output to the standard out, the process
						 * is successfully terminated. */
						(c, o) -> "bye".equals(o),
						/* If a message is printed to the error out, let the process be 
						 * killed forcibly. */
						(c, o) -> {
							success.set(false);
							return true;
						}), false))) {
					// If success is false, the process will be terminated forcibly
					return success.get();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// If the something went wrong, the process is terminated forcibly.
			return false;
		}
		@Override
		public void onTermination(int resultCode) {
			// Don't do anything after a process has terminated.
		}
		
	}, 10, 50, 5, 60000, true);

In the example above, a *StandardProcessPoolExecutor* for a program called "test.exe" is created. Every time the pool starts a new process, it waits until the message "hi" is output to the process' standard out, signaling that it has started up, then the pool sends the instruction "start" to the process' standard in. The instruction "start" has the process sleep for 1 second before it outputs "ok". Once this message is output to the process' standard out, the pool considers the process ready for submissions. Whenever the process needs to be terminated (either due to timing out or having it cancelled after the execution of a submission), the pool tries to terminate the process in an orderly way by sending it the "stop" instruction. If the response to this is "bye", the process is considered terminated. However, if something is printed to the process' error out in response to the "stop" instruction, the process is killed forcibly. The pool's minimum size is 10, its maximum size is 50, its reserve size is 5, its idle processes are terminated after 1 minute, and it is verbose (meaning that all events related to the management of the pool are logged to the console).

	List<Future<?>> futures = new ArrayList<>();
	// Have 30 "process 5" commands be submitted to the pool in 3 seconds.
	for (int i = 0; i < 30; i++) {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		futures.add(pool.submit(new SimpleSubmission(new SimpleCommand("process 5",
				(s, o) -> {
					/* If the response is "ready", print out everything written to the 
					 * process' standard out in response to to the instruction. */
					if ("ready".equals(o)) {
						System.out.println(s.getJointStandardOutLines());
						return true;
					}
					return false;
				},
				/* If the response is sent to the process' error out, print out everything 
				 * written to its error out. */
				(s, o) -> {
					System.out.println(s.getJointErrorOutLines());
					return true;
				}), true)));
	}
	// Wait for the submission to be processed.
	for (Future<?> future : futures) {
		try {
			future.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
	}
	// Shut down the pool.
	pool.shutdown();

Once the pool is initialized, it is sent 30 instructions within 3 seconds. The instruction "process 5" has the process sleep for 5 seconds, printing "in progress" to is standard out every second except for the 5th second, when it prints "ready". The *Submission* above also has the process that executed it cancelled afterwards as denoted by the second, boolean parameter of the constructor of [*SimpleSubmission*](https://viktorc.github.io/PPE4J/net/viktorc/ppe4j/SimpleSubmission). As the pool receives the submissions, it manages its size according to its minimum, maximum, and reserve size parameters.

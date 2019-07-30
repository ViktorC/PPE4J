package net.viktorc.pp4j.impl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * The class whose main method is run as a separate process by the Java process pool executor.
 *
 * @author Viktor Csomor
 */
public class JavaProcess {

  /**
   * Redirects the process' standard out stream.
   *
   * @param outputStream The stream the standard output should be redirected to.
   */
  private static void redirectStdOut(PrintStream outputStream) {
    try {
      System.setOut(outputStream);
    } catch (SecurityException e) {
      // Ignore it.
    }
  }

  /**
   * Redirects the process' standard error stream.
   *
   * @param errorStream The stream the standard error should be redirected to.
   */
  private static void redirectStdErr(PrintStream errorStream) {
    try {
      System.setErr(errorStream);
    } catch (SecurityException e) {
      // Ignore it.
    }
  }

  /**
   * The method executed as a separate process. It listens to its standard in for encoded and serialized {@link Callable} instances,
   * which it decodes, deserializes, and executes on receipt. The return value is normally output to its stdout stream in the form of the
   * serialized and encoded return values of the <code>Callable</code> instances. If an exception occurs, it is serialized, encoded, and
   * output to the stderr stream.
   *
   * @param args They are ignored for now.
   */
  public static void main(String[] args) {
    PrintStream originalOut = System.out;
    try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in, JavaObjectCodec.CHARSET));
        DummyPrintStream dummyOut = new DummyPrintStream();
        DummyPrintStream dummyErr = new DummyPrintStream()) {
      /* As the process' stderr is redirected to the stdout, there is no need for a
       * reference to this stream; neither should the submissions be able to print to
       * stdout through the System.err stream. */
      redirectStdErr(dummyErr);
      // Send the startup signal.
      System.out.println(JavaObjectCodec.getInstance().encode(Signal.READY));
      try {
        for (;;) {
          try {
            String line = in.readLine();
            if (line == null) {
              return;
            }
            line = line.trim();
            if (line.isEmpty()) {
              continue;
            }
            Object input = JavaObjectCodec.getInstance().decode(line);
            if (input == Request.TERMINATE) {
              System.out.println(JavaObjectCodec.getInstance().encode(Signal.TERMINATED));
              return;
            } else if (input instanceof Callable<?>) {
              Callable<?> c = (Callable<?>) input;
              /* Try to redirect the out stream to make sure that print
               * statements and such do not cause the submission to be assumed
               * done falsely. */
              redirectStdOut(dummyOut);
              Object output = c.call();
              redirectStdOut(originalOut);
              System.out.println(JavaObjectCodec.getInstance().encode(new Response(false, output)));
            }
          } catch (Throwable e) {
            redirectStdOut(originalOut);
            System.out.println(JavaObjectCodec.getInstance().encode(new Response(true, e)));
          }
        }
      } catch (Throwable e) {
        redirectStdOut(dummyOut);
        throw e;
      }
    } catch (Throwable e) {
      try {
        System.out.println(JavaObjectCodec.getInstance().encode(new Response(true, e)));
      } catch (Exception e1) {
        // Give up all hope.
      }
    }
  }

  /**
   * A dummy implementation of {@link PrintStream} that does nothing on <code>write</code>.
   *
   * @author Viktor Csomor
   */
  private static class DummyPrintStream extends PrintStream {

    DummyPrintStream() {
      super(new OutputStream() {

        @Override
        public void write(int b) {
          // This is why it is called "DummyPrintStream".
        }
      });
    }

  }

  /**
   * An enum representing requests that can be sent to the Java process.
   */
  public enum Request {
    TERMINATE
  }

  /**
   * An enum representing signals that the Java process can send back to the parent process.
   */
  public enum Signal {
    READY,
    TERMINATED
  }

  /**
   * A simple class to encapsulate the response of the Java process to a task.
   */
  public static class Response implements Serializable {

    private boolean error;
    private Object result;

    /**
     * Constructs a <code>Response</code> object.
     *
     * @param error Whether the result is an exception thrown during execution.
     * @param result The result of the submission.
     */
    private Response(boolean error, Object result) {
      this.error = error;
      this.result = result;
    }

    /**
     * Returns whether the result is an exception or error thrown during the execution of the task.
     *
     * @return Whether the result is an instance of {@link Throwable} thrown during the execution of the task.
     */
    public boolean isError() {
      return error;
    }

    /**
     * Returns the result of the task or the {@link Throwable} thrown during the execution of the task.
     *
     * @return The result of the task.
     */
    public Object getResult() {
      return result;
    }

  }

}

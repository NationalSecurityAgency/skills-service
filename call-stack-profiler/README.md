# Call Stack Profiler for Groovy 

> Profile your code with negligible performance and memory overhead.

This profiler keeps track of the method calls and outputs method call hierarchy, allowing developers 
to quickly comprehend execution time breakdown. 
- The profiler is **fast** and is appropriate to have track and output enabled in a production system
- Use ``@Profile`` to easily annotate Groovy methods OR wrap logic in a closure OR manually start/stop events
- Naturally fits into a service based architecture
- Provides support for delegating concurrent tasks to a Thread Pool 

Consider the following class where methods are annotated with the ``@Profile`` annotation:
```groovy
class Example {
    @Profile
    void m1() {
        m2()
    }

    @Profile
    void m2() {
        5.times {
            m3()
        }
    }

    @Profile
    void m3() {
        Thread.sleep(100)
    }
}
```

and then

```groovy
static void main(String[] args) {
   new Example().m1()
   println CProf.prettyPrint()
}
```

then the output is: 
```
|-> Example.m1 (1) : 501ms [000ms]
|     |-> Example.m2 (1) : 501ms [000ms]
|     |     |-> Example.m3 (5) : 501ms
```

The output provides method call hierarchy as well as the following information:
- Total method execution time: number in ms, seconds and/or minutes 
- ``(N)``: number of times method was called, m2() was called once and m3() called 5 times
- ``[N ms]``: execution time which was not accounted for by child methods/logic; this happens when either not all of the child methods/logic is profiled OR there is a GC or JVM overhead

## Features

### Custom Profile Name

When using the ``@Profile`` annotation, by default, profile names are derived from the method name and its parameters. 
You can supply a custom name by setting the ``name`` attribute on the ``@Profile`` annotation:

```groovy
class Example {
    @Profile(name = 'veryCustomName')
    void m1() {
        m2()
    }

    @Profile
    void m2() {
        Thread.sleep(20)
    }
}
```

Then the output is:

```
|-> veryCustomName (1) : 020ms [000ms]
|     |-> Example.m2 (1) : 020ms
```

### Closure based Profiling

You can easily profile (and name) any bit of code by wrapping it in a closure:

```groovy
class Example {
    @Profile
    void m1() {
        m2()
        CProf.prof("Another Long Action") {
            // great logic
            Thread.sleep(1000)
        }
    }

    @Profile
    void m2() {
        Thread.sleep(20)
    }
}
```

Then the output is:
```
|-> Example.m1 (1) : 1s 020ms [000ms]
|     |-> Example.m2 (1) : 020ms
|     |-> Another Long Action (1) : 1s 
```

### Manually start/stop events

Start and stop profiling events can be managed manually:

```groovy
class Example {
    @Profile
    void m1() {
        m2()
        String name = "Another Long Action"
        CProf.start(name)
        try {
            // great logic
            Thread.sleep(1000)
        } finally {
            CProf.stop(name)
        }
    }

    @Profile
    void m2() {
        Thread.sleep(20)
    }
}
```

Then the output is:
```
|-> Example.m1 (1) : 1s 020ms [000ms]
|     |-> Example.m2 (1) : 020ms
|     |-> Another Long Action (1) : 1s 
```

If you select to manually manage start/stop events then please:
- always wrap logic in a ``try/catch`` block to ensure the event is closed
- verify that the same name is used to start and end the event 

### Delegate concurrent tasks to a Thread Pool

Call Stack Profiler supplies a thread pool implementation ``ProfThreadPool`` 
which makes it seamless to execute and profile concurrent tasks.

Below is an example of executing methods ``m1()`` and ``(m2)`` concurrently:

```groovy
class Example {
    @Profile
    void runConcurrent() {
        ProfThreadPool threadPool = new ProfThreadPool("Threads", 2, 2)
        threadPool.warnIfFull = false
        List<Callable<Integer>> callables = [
                ThreadPoolUtils.callable {
                    m1()
                },
                ThreadPoolUtils.callable {
                    m2()
                },
        ]
        List<Integer> res = threadPool.asyncExec(callables)
        println "Result: ${res}"
    }

    @Profile
    int m1() {
        5.times { m2() }
        return 10
    }

    @Profile()
    int m2() {
        Thread.sleep(20)
        return 5
    }
}
```

Then the output is:
```
Result: [10, 5]
|-> Example.runConcurrent (1) : 104ms [003ms]
|     ||-> Example.m1-Threads-1 (1) : 101ms [000ms]
|     ||     |-> Example.m2 (5) : 101ms
|     ||-> Example.m2-Threads-2 (1) : 020ms 
```

``||`` depicts that the code is being executed concurrently  

### Each Call as its own event

If you are calling a method within a loop AND the loop has a reasonable (for display purposes) number of elements, 
then you may want to opt for displaying each method call as its own profiling event. 

Set attribute ``aggregateIntoSingleEvent = false`` for the ``@Profile`` annotation, for example:
 
```groovy
class Example {
    @Profile
    void m1() {
        5.times {
            m2()
        }
    }

    @Profile(aggregateIntoSingleEvent = false)
    void m2() {
        Thread.sleep(20)
    }
}
```  

Then the output is:
```
|-> Example.m1 (1) : 102ms [000ms]
|     |-> Example.m20_24 (1) : 021ms
|     |-> Example.m20_23 (1) : 020ms
|     |-> Example.m20_22 (1) : 020ms
|     |-> Example.m20_21 (1) : 021ms
|     |-> Example.m20_20 (1) : 020ms
```

### Exceptions

Exceptions are propagated as expected. For example:

```groovy
class Example {
    @Profile
    int m1() {
        5.times { m2() }
        return 10
    }

    @Profile()
    int m2() {
        throw new RuntimeException("It's fun to fail!")
    }
}
```

Then the output is:
```
Exception in thread "main" java.lang.RuntimeException: It's fun to fail!
	at callStack.profiler.examples.Example.m2(Example.groovy:15)
...
...
```

### Entry method

At runtime, profiling starts when the very first profiling artifact is encountered, which can be one of these:
- ``@Profile`` annotation
- ``Cprof.prof`` method
- ``CProf.start`` method

If the same entry point is encountered again then the profiling restarts/resets (there can only be one entry point). 
Please consider:

```groovy
class Example {
    @Profile
    int entryPoint() {
        5.times { m2() }
        return 10
    }

    @Profile()
    int m2() {
        Thread.sleep(200)
        return 5
    }
}
```
and then:
```groovy
class ForDocs {
    static void main(String[] args) {
        5.times {
            new Example().entryPoint()
        }
        println CProf.prettyPrint()
    }
}
```

The output is then:
```
|-> Example.entryPoint (1) : 1s 001ms [000ms]
|     |-> Example.m2 (5) : 1s 001ms
```

``entryPoint()`` is the first time a profiling event is discovered, so each time the profiler encounters the entry point method it resets its profiling stack. 
Let's move ``CProf.prettyPrint()`` into the loop:
```groovy
class ForDocs {
    static void main(String[] args) {
        5.times {
            new Example().entryPoint()
            println CProf.prettyPrint()
        }
    }
}
```
Now the output is: 
```
|-> Example.entryPoint (1) : 1s 011ms [001ms]
|     |-> Example.m2 (5) : 1s 010ms
|-> Example.entryPoint (1) : 1s 001ms [000ms]
|     |-> Example.m2 (5) : 1s 001ms
|-> Example.entryPoint (1) : 1s 001ms [000ms]
|     |-> Example.m2 (5) : 1s 001ms
|-> Example.entryPoint (1) : 1s 003ms [001ms]
|     |-> Example.m2 (5) : 1s 002ms
|-> Example.entryPoint (1) : 1s 001ms [000ms]
|     |-> Example.m2 (5) : 1s 001ms
```

### Access Profile Stack Programmatically   

Instead of using ``CProf.prettyPrint()`` you can get a hold of the entry event programmatically via ``CProf.rootEvent`` and then store the results anywhere you want. 
For example: 
```groovy
ProfileEvent entryEvent = CProf.rootEvent
// grab child events
entryEvent.children.each {
    // use these accessors
    it.getName()
    it.getNumOfInvocations()
    it.getRuntimeInMillis()
    it.isConcurrent()
    it.isRemote()
}
```

## How does it work?

Call Stack profiler utilizes Groovy's (Abstract Syntax Tree) AST Transformation to inject profiling code into the annotated methods.
Profiling code is injected during the compilation phase so there is no introspection at runtime which accounts for the minimal overhead. 

For example take the following code:

```groovy
    @Profile()
    int m2() {
        return 5
    }
```

will be compiled into something like this:

```groovy
    int m2() {
        String profName = "m2"
        CProf.start(profName)
        try {
            return 5
        } finally {
            CProf.stop(profName)    
        }
    }
```

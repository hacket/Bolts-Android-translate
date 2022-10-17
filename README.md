## Bolts-Android 中文翻译

[Bolts-Android](https://github.com/BoltsFramework/Bolts-Android)

### 准备

github：
https://github.com/BoltsFramework/Bolts-Android

加入gradle
```groovy
compile 'com.parse.bolts:bolts-tasks:1.4.0'
```

原文：
https://github.com/BoltsFramework/Bolts-Android/blob/master/README.md

`注`只翻译了Bolts-Android部分，[bolts-applinks](https://github.com/BoltsFramework/Bolts-Android/tree/master/bolts-applinks "bolts-applinks")没有翻译。

### Tasks
创建一个真正及时响应的Android应用，必须保证耗时的操作在非UI线程中，避免任何让UI线程等待阻塞的事情。这意味着你需要在后台执行各种操作。为了让这种操作更简单，我们需要一个叫`Task`的类。一个`Task`代表一个异步操作。典型的，一个`Task`作为异步函数的返回，它有能力继续处理这个任务的结果。当一个`Task`作为一个函数的返回，表示它已经开始工作了。一个`Task`没有关联特定的线程模型：它代表的是所做的工作，不是在哪执行。`Task`s优于其他异步操作的方式，比如`callback`s和`AsyncTask`。
 * 消耗更少的系统资源，它们不再占用一个线程直到等待其他`Task`s
 * 连续执行数个任务不会像只使用回调函数时那样创建嵌套的“金字塔（pyramid）”代码。
 * `Task`s是完全可组合的，允许开发人员执行分支、并行和复杂的错误处理，不像意大利式的代码需要很多`callback`s。
 *  开发人员可以按照执行顺序安排基于任务的代码，而不必将逻辑分解到分散的回调函数中。
 
### The `continueWith` Method
每个`Task`有一个叫做`continueWith`的带`Continuation`参数的方法。`Continuation`是一个接口，只有一个必须要实现的方法，叫做`then`。`continueWith`是前一个`Task`执行完毕后会调用`then`方法。你能检查这个`Task`是否执行成功，得到它的执行结果。
```java
Task.callInBackground(new Callable<String>() {
    @Override
    public String call() throws Exception {
        Log.d(TAG, "call,before:" + System.currentTimeMillis());
        SystemClock.sleep(1000);
        Log.d(TAG, "call,after:" + System.currentTimeMillis());
        return "www.baidu.com";
    }
}).continueWith(new Continuation<String, Integer>() {
    @Override
    public Integer then(Task<String> task) throws Exception {
        if (task.isCancelled()) {
            // the previous task was cancelled
            Log.d(TAG, "then: cancelled");
        } else if (task.isFaulted()) {
            // the previous task was failed
            Exception exception = task.getError();
            Log.d(TAG, "then: faulted " + exception.getMessage());
        } else {
            // the previous task was successfully
            String result = task.getResult();
            Log.d(TAG, "then,success:" + result);
        }
        return null;
    }
});
}
```
在大多数情况下，你仅仅需要做一些操作如果之前的`Task`执行成功，这样的话，用`onSuccess`方法替代`continueWith`。
上面Task执行成功等同于：
```java
Task.callInBackground(new Callable<String>() {
    @Override
    public String call() throws Exception {
        Log.d(TAG, "call,before:" + System.currentTimeMillis());
        SystemClock.sleep(1000);
        Log.d(TAG, "call,after:" + System.currentTimeMillis());
        return "www.baidu.com";
    }
}).onSuccess(new Continuation<String, Integer>() {

    @Override
    public Integer then(Task<String> task) throws Exception {
        // the previous task was successfully
        String result = task.getResult();
        Log.d(TAG, "then,success2:" + result);
        return null;
    }
});
```
### Chaining Tasks Together
`Task`s有点不可思议，因为它们可以让你将它们链起来而不需要嵌套。如果你用`continueWithTask`替代`continueWith`，你能得到一个新的Task。用`continueWith/onSuccess`做同步的工作，`continueWithTask/onSuccessTask`做异步的工作
```java
Task.callInBackground(new Callable<String>() {
    @Override
    public String call() throws Exception {
        Log.d(TAG, "【call4】: thread" + Thread.currentThread().getName());
        SystemClock.sleep(1000);
        return "www.qq.com";
    }
}).onSuccessTask(new Continuation<String, Task<Integer>>() {

    @Override
    public Task<Integer> then(Task<String> task) throws Exception {
        return saveAsync(task.getResult());
    }
}).onSuccessTask(new Continuation<Integer, Task<String>>() {

    @Override
    public Task<String> then(Task<Integer> task) throws Exception {
        return findAsync(task.getResult());
    }
}).onSuccessTask(new Continuation<String, Task<Integer>>() {

    @Override
    public Task<Integer> then(Task<String> task) throws Exception {
        return saveAsync(task.getResult());
    }
}).onSuccess(new Continuation<Integer, Void>() {

    @Override
    public Void then(Task<Integer> task) throws Exception {
        // evetything is done!
        Log.d(TAG, "then: final success!!!" + task.getResult());
        return null;
    }
});
private Task<String> findAsync(Integer result) {
    Log.d(TAG, "findAsync: " + result);
    return Task.call(new Callable<String>() {
        @Override
        public String call() throws Exception {
            Log.d(TAG, "findAsync call:");
            return "hacket";
        }
    });
}

private Task<Integer> saveAsync(String result) {
    Log.d(TAG, "saveAsync: " + result);
    return Task.call(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            Log.d(TAG, "saveAsync call:");
            return 100;
        }
    });
}
```

### Error Handling
谨慎选择是否需要调用`continueWith`或者`onSuccess`，你能控制error如何在你的应用传递。用`continueWith`自己处理错误。你能抛出一个异常来让你的Task失败。
```java
Task.callInBackground(new Callable<String>() {
    @Override
    public String call() throws Exception {
        Log.d(TAG, "【call5】: thread" + Thread.currentThread().getName());
        SystemClock.sleep(1000);
        return "www.baidu.com";
    }
}).onSuccessTask(new Continuation<String, Task<Integer>>() {
    @Override
    public Task<Integer> then(Task<String> task) throws Exception {
        Log.d(TAG, "then: onSuccessTask");
        throw new RuntimeException("There was an error!");
    }
}).continueWithTask(new Continuation<Integer, Task<String>>() {
    @Override
    public Task<String> then(Task<Integer> task) throws Exception {
        Log.d(TAG, "then: continueWithTask：" + task.isFaulted());
        return null;
    }
}).onSuccess(new Continuation<String, Void>() {
    @Override
    public Void then(Task<String> task) throws Exception {
        Log.d(TAG, "then: success!!!");
        return null;
    }
});
```
### Creating Tasks
创建一个`TaskCompletionSource`，它可以让你创建一个新的`Task`并且控制它是否已经完成或者取消。当你创建一个`Task`后，你需要调用`setResult`，`setError`，`setCancelled`触发它的配置
```java
//        succeedAsync().onSuccess(new Continuation<String, Void>() {
failAsync().onSuccess(new Continuation<String, Void>() {
    @Override
    public Void then(Task<String> task) throws Exception {
        Log.d(TAG, "then: onSuccess:" + task.getResult());
        return null;
    }
}).continueWith(new Continuation<Void, Void>() {
    @Override
    public Void then(Task<Void> task) throws Exception {
        Log.d(TAG, "then: continueWith");
        return null;
    }
});
public Task<String> failAsync() {
    TaskCompletionSource<String> failed = new TaskCompletionSource<>();
    failed.setError(new RuntimeException("An error message."));
    return failed.getTask();
}

public Task<String> succeedAsync() {
    TaskCompletionSource<String> sufTask = new TaskCompletionSource<String>();
    sufTask.setResult("this is good result!");
    return sufTask.getTask();
}
```
如果你知道这个`Task`的结果在创建的时候，你能用下面方便的方法：
```java
Task<String> successful = Task.forResult("The good result.");

Task<String> failed = Task.forError(new RuntimeException("An error message."));
```

### Creating Async Methods
很容易创建一个异步函数的`Task`，例如
```java
public Task<ParseObject> fetchAsync(ParseObject obj) {
  final TaskCompletionSource<ParseObject> tcs = new TaskCompletionSource<>();
  obj.fetchInBackground(new GetCallback() {
    public void done(ParseObject object, ParseException e) {
     if (e == null) {
       tcs.setResult(object);
     } else {
       tcs.setError(e);
     }
   }
  });
  return tcs.getTask();
}
```
我们也提供了方便的函数帮助你创建`Task`s，`callInBackground`在后台线程池跑一个`Task`，当调用到`call`立即执行里面的代码。
```java
Task.callInBackground(new Callable<Void>() {
  public Void call() {
    // Do a bunch of stuff.
  }
}).continueWith(...);
```

### Tasks in Series
执行一系列异步操作，每一个等待前一个的执行完毕。
```java
ParseQuery<ParseObject> query = ParseQuery.getQuery("Comments");
query.whereEqualTo("post", 123);

findAsync(query).continueWithTask(new Continuation<List<ParseObject>, Task<Void>>() {
  public Task<Void> then(Task<List<ParseObject>> results) throws Exception {
    // Create a trivial completed task as a base case.
    Task<Void> task = Task.forResult(null);
    for (final ParseObject result : results) {
      // For each item, extend the task with a function to delete the item.
      task = task.continueWithTask(new Continuation<Void, Task<Void>>() {
        public Task<Void> then(Task<Void> ignored) throws Exception {
          // Return a task that will be marked as completed when the delete is finished.
          return deleteAsync(result);
        }
      });
    }
    return task;
  }
}).continueWith(new Continuation<Void, Void>() {
  public Void then(Task<Void> ignored) throws Exception {
    // Every comment was deleted.
    return null;
  }
});
```
### Tasks in Parallel
并行的执行一系列`Task`s，用`whenAll`方法。你能同时开始多种操作，用`Task.whenAll`创建一个新的`Task`，这个Task当所有的输入Tasks完成时会标记为完成状态。这个新的Task会成功当所有的输入Tasks执行成功。并行的执行会比`series`的快，但会消耗更多的系统资源和带宽。
```java
private void test7() {
    createTasks().continueWithTask(new Continuation<List<String>, Task<Void>>() {

        @Override
        public Task<Void> then(Task<List<String>> results) throws Exception {

            Log.d(TAG, "then: continueWithTask begin");

            List<Task<String>> tasks = new ArrayList<Task<String>>(results.getResult().size());
            for (int i = 0; i < results.getResult().size(); i++) {
                final int j = i;
                tasks.add(Task.call(new Callable<String>() {
                    @Override
                    public String call() throws Exception {
                        Log.d(TAG, "this is the task:" + j);
                        return "this is the task:" + j;
                    }
                }));
            }

            Log.d(TAG, "then: continueWithTask end");

            return Task.whenAll(tasks);
        }
    }).continueWith(new Continuation<Void, Void>() {
        @Override
        public Void then(Task<Void> task) throws Exception {
            Log.d(TAG, "then: continueWith");
            return null;
        }
    });
}

private Task<List<String>> createTasks() {
    List<String> listTasks = new ArrayList<String>();
    for (int i = 0; i < 5; i++) {
        listTasks.add("this is the " + i + " task");
    }
    return Task.forResult(listTasks);
}
```
### Task Executors
所有的`continueWith`和`onSuccess`方法第2个参数可以带一个`java.util.concurrent.Executor`。这允许我们控制这continuation如何执行。`Task.call()`默认执行`Callable`s在当前线程，`Task.callInBackground`会用它自己的线程池，但是你能用自己的线程池来安排任务在不同的线程。
```java
static final Executor NETWORK_EXECUTOR = Executors.newCachedThreadPool();
static final Executor DISK_EXECUTOR = Executors.newCachedThreadPool();

final Request request = ...
Task.call(new Callable<HttpResponse>() {
  @Override
  public HttpResponse call() throws Exception {
    // Work is specified to be done on NETWORK_EXECUTOR
    return client.execute(request);
  }
}, NETWORK_EXECUTOR).continueWithTask(new Continuation<HttpResponse, Task<byte[]>>() {
  @Override
  public Task<byte[]> then(Task<HttpResponse> task) throws Exception {
    // Since no executor is specified, it's continued on NETWORK_EXECUTOR
    return processResponseAsync(response);
  }
}).continueWithTask(new Continuation<byte[], Task<Void>>() {
  @Override
  public Task<Void> then(Task<byte[]> task) throws Exception {
    // We don't want to clog NETWORK_EXECUTOR with disk I/O, so we specify to use DISK_EXECUTOR
    return writeToDiskAsync(task.getResult());
  }
}, DISK_EXECUTOR);
```
Bolts提供了3种EXECUTOR：
```java
public static final ExecutorService BACKGROUND_EXECUTOR = BoltsExecutors.background(); // 后台线程池

private static final Executor IMMEDIATE_EXECUTOR = BoltsExecutors.immediate(); // 在当前线程，如果stack太深，最多15，超过会委派到BACKGROUND_EXECUTOR中去。

 public static final Executor UI_THREAD_EXECUTOR = AndroidExecutors.uiThread(); // UI线程
```

### Capturing Variables
Java允许函数"capture"变量从外部作用域，但是它们要被标记为`final`,并且不可变。这是很不方便的。这也是为什么我们添加一个方便的类`Capture`，它能共享一个可变的变量在你的callbakcs。仅仅调用`get`和`set`去获取和改变它的值。
```java
final Capture<Integer> captureVariable = new Capture<Integer>(0);

private void test9() {
    Task.call(new Callable<Integer>() {
        @Override
        public Integer call() throws Exception {
            Integer integer = captureVariable.get();
            integer += 10;
            captureVariable.set(integer);
            return integer;
        }
    }, Task.BACKGROUND_EXECUTOR).continueWith(new Continuation<Integer, Integer>() {
        @Override
        public Integer then(Task<Integer> task) throws Exception {
            int value = captureVariable.get();
            value += 100;
            captureVariable.set(value);
            return value;
        }
    }, Task.UI_THREAD_EXECUTOR).onSuccess(new Continuation<Integer, Void>() {
        @Override
        public Void then(Task<Integer> task) throws Exception {
            int value = captureVariable.get();
            Log.d(TAG, "then: value=" + value); // 110
            return null;
        }
    }, Task.BACKGROUND_EXECUTOR);
}
```

### Cancelling Tasks
取消一个`Task`，通过创建一个`CancellationTokenSource`，传递匹配的token去创建那个你想取消的`Task`，然后调用`cancel`方法。这样可以通过token来取消正在进行的`Task`s
```java
CancellationTokenSource cts = new CancellationTokenSource();

Task<Integer> task = getIntAsync(cts.getToken());
task.onSuccess(new Continuation<Integer, Void>() {
    @Override
    public Void then(Task<Integer> task) throws Exception {
        Log.d(TAG, "then: result:" + task.getResult());
        return null;
    }
});

cts.cancel();


/**
 * Gets an Integer asynchronously.
 */
public Task<Integer> getIntAsync(final CancellationToken ct) {
    // Create a new Task
    final TaskCompletionSource<Integer> tcs = new TaskCompletionSource<>();

    new Thread() {
        @Override
        public void run() {
            // Check if cancelled at start
            if (ct.isCancellationRequested()) {
                tcs.setCancelled();
                return;
            }

            int result = 0;
            while (result < 100) {
                // Poll isCancellationRequested in a loop
                if (ct.isCancellationRequested()) {
                    tcs.setCancelled();
                    return;
                }
                result++;

                SystemClock.sleep(1000);

                Log.d(TAG, "run: result:" + result);

            }
            tcs.setResult(result);
        }
    }.start();

    return tcs.getTask();
}
```

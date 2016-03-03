package com.zengfansheng.boltsandroiddemo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import bolts.CancellationToken;
import bolts.CancellationTokenSource;
import bolts.Capture;
import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //        test1();
        //        test2();
        //        test3();

        //        test4();

        //        test5();

        //        test6();

        //        test7();

        //        test8();

        //        test9();

        test10();
    }

    private void test10() {
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
    }

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

    private void test8() {
        Task.call(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.d(TAG, "call1:" + Thread.currentThread().getName());
                return null;
            }
        }, Task.BACKGROUND_EXECUTOR);

        Task.call(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.d(TAG, "call2:" + Thread.currentThread().getName());
                return null;
            }
        });
    }

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

    private void test6() {

        Task.forResult("hh");

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
    }

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

    private void test5() {
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
                return findAsync(200);
            }
        }).onSuccess(new Continuation<String, Void>() {
            @Override
            public Void then(Task<String> task) throws Exception {
                Log.d(TAG, "then: success!!!" + task.getResult());
                return null;
            }
        });
    }

    private void test4() {
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
                //                return saveAsync(task.getResult());
                Log.d(TAG, "then: onSuccessTask1");
                throw new RuntimeException("There was an error1!");
            }
        }).onSuccessTask(new Continuation<Integer, Task<String>>() {

            @Override
            public Task<String> then(Task<Integer> task) throws Exception {
                //                return findAsync(task.getResult());
                Log.d(TAG, "then: onSuccessTask2");
                throw new RuntimeException("There was an error!2");
            }
        }).onSuccessTask(new Continuation<String, Task<Integer>>() {

            @Override
            public Task<Integer> then(Task<String> task) throws Exception {
                //                return saveAsync(task.getResult());
                Log.d(TAG, "then: onSuccessTask3");
                throw new RuntimeException("There was an error!3");
            }
        }).onSuccess(new Continuation<Integer, Void>() {

            @Override
            public Void then(Task<Integer> task) throws Exception {
                // evetything is done!
                Log.d(TAG, "then: final success!!!" + task.getResult());
                return null;
            }
        });
    }

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

    private void test3() {
        Log.d(TAG, "test3: ============================");
        for (int i = 0; i < 5; i++) {
            final int j = i;
            Task.callInBackground(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    Log.d(TAG, "【call3】: thread" + Thread.currentThread().getName() + "===========" + j);
                    SystemClock.sleep(1000);

                    return "www.baidu.com";
                }
            }).continueWithTask(new Continuation<String, Task<Integer>>() {
                @Override
                public Task<Integer> then(Task<String> task) throws Exception {
                    Log.d(TAG, "【then3】: thread" + Thread.currentThread().getName() + "===========" + j);
                    if (task.isCancelled()) {
                        Log.d(TAG, "then3: cancelled");
                    } else if (task.isFaulted()) {
                        Log.d(TAG, "【then3】: faulted:" + task.getError().getMessage());
                    } else {
                        //                        Log.d(TAG, "【then3:】 successed:" + task.getResult());
                    }

                    return Task.call(new Callable<Integer>() {
                        @Override
                        public Integer call() throws Exception {
                            Log.d(TAG, "【continueWithTask】 call thread：" + Thread.currentThread().getName() +
                                    "===========" + j);
                            int i = 10 / 0;
                            return 200;
                        }
                    });
                }
            }).onSuccess(new Continuation<Integer, String>() {
                @Override
                public String then(Task<Integer> task) throws Exception {
                    Log.d(TAG, "then: onSuccess" + task.getResult());
                    return null;
                }
            });
        }
    }

    private void test2() {
        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.d(TAG, "call1: thread" + Thread.currentThread().getName());
                Log.d(TAG, "call,before:" + System.currentTimeMillis());
                SystemClock.sleep(1000);
                Log.d(TAG, "call,after:" + System.currentTimeMillis());
                return "www.baidu.com";
            }
        }).onSuccess(new Continuation<String, Integer>() {

            @Override
            public Integer then(Task<String> task) throws Exception {
                Log.d(TAG, "then1: thread" + Thread.currentThread().getName());
                // the previous task was successfully
                String result = task.getResult();
                Log.d(TAG, "then,success2:" + result);
                return null;
            }
        });
    }

    private void test1() {
        Task.callInBackground(new Callable<String>() {
            @Override
            public String call() throws Exception {
                Log.d(TAG, "call2: thread" + Thread.currentThread().getName());
                Log.d(TAG, "call,before:" + System.currentTimeMillis());
                SystemClock.sleep(1000);
                Log.d(TAG, "call,after:" + System.currentTimeMillis());
                return "www.baidu.com";
            }
        }).continueWith(new Continuation<String, Integer>() {
            @Override
            public Integer then(Task<String> task) throws Exception {
                Log.d(TAG, "then2: thread" + Thread.currentThread().getName());
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

}

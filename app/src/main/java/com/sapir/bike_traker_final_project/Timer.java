package com.sapir.bike_traker_final_project;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {
    public interface KillTicker {
        void killMe(Task task);
    }

    public interface CycleTicker {
        void periodic(int repeatsRemaining);
        void done();
    }

    public interface OneTimeTicker {
        void done();
    }

    public class Task {
        private ScheduledExecutorService scheduleTaskExecutor;
        CycleTicker cycleTickerCallback;
        OneTimeTicker oneTimeTicker;
        KillTicker killTickerCallBack;
        String tag;
        int repeats;
        boolean IM_DONE = false;

        public Task(KillTicker killTickerCallBack, CycleTicker cycleTickerCallback, int repeats, int periodInMilliseconds, String tag) {
            this.killTickerCallBack = killTickerCallBack;
            this.cycleTickerCallback = cycleTickerCallback;
            this.repeats = repeats;
            this.tag = tag;

            scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
            scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    tickerFunction();
                }
            }, 0, periodInMilliseconds, TimeUnit.MILLISECONDS);
        }

        public Task(KillTicker killTickerCallBack, OneTimeTicker oneTimeTicker, int delayInMilliseconds, String tag) {
            this.killTickerCallBack = killTickerCallBack;
            this.oneTimeTicker = oneTimeTicker;
            this.repeats = 1;
            this.tag = tag;

            scheduleTaskExecutor = Executors.newScheduledThreadPool(5);
            scheduleTaskExecutor.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    singleTickerFunction();
                }
            }, delayInMilliseconds, 1, TimeUnit.MILLISECONDS);
        }

        private void singleTickerFunction() {
            oneTimeTicker.done();
            killMe();
        }

        private void tickerFunction() {
            // send done function second after last call
            if (IM_DONE) {
                cycleTickerCallback.done();
                killMe();
            }
            else {
                cycleTickerCallback.periodic(repeats);

                if (!(repeats == CONTINUOUSLY_REPEATS)) {
                    repeats--;
                    if (repeats <= 0) {
                        IM_DONE = true;
                    }
                }
            }
        }

        public void killMe() {
            try {
                scheduleTaskExecutor.shutdown();
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
            scheduleTaskExecutor = null;
            cycleTickerCallback = null;
            killTickerCallBack.killMe(this);
        }
    }

    public static final int CONTINUOUSLY_REPEATS = -999;

    private ArrayList<Task> tasks;
    private Object locker = new Object();
    private static Timer instance;

    KillTicker killTickerCallBack = new KillTicker() {
        @Override
        public void killMe(Task task) {
            synchronized (locker) {
                tasks.remove(task);
            }
        }
    };

    public static Timer get() {
        return instance;
    }

    public static Timer initHelper() {
        if (instance == null)
            instance = new Timer();
        return instance;
    }

    private Timer() {
        tasks = new ArrayList<>();
    }

    public int getNumOfActiveTickers() {
        synchronized (locker) {
            return tasks.size();
        }
    }

    public void cycle(CycleTicker cycleTicker, int repeats, int periodInMilliseconds, String tag) {
        if (repeats == CONTINUOUSLY_REPEATS || repeats > 0) {
            synchronized (locker) {
                tasks.add(new Task(killTickerCallBack, cycleTicker, repeats, periodInMilliseconds, tag));
            }
        }
    }

    public void single(OneTimeTicker oneTimeTicker, int delayInMilliseconds, String tag) {
        synchronized (locker) {
            tasks.add(new Task(killTickerCallBack, oneTimeTicker, delayInMilliseconds, tag));
        }
    }

    public void cycle(CycleTicker cycleTicker, int repeats, int periodInMilliseconds) {
        cycle(cycleTicker, repeats, periodInMilliseconds, "");
    }

    public void single(OneTimeTicker oneTimeTicker, int delayInMilliseconds) {
        single(oneTimeTicker, delayInMilliseconds,"");
    }

    public void remove(CycleTicker cycleTicker) {
        synchronized (locker) {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                if (tasks.get(i).cycleTickerCallback == cycleTicker) {
                    tasks.get(i).killMe();
                    // break; Continue loop because there may be more duplicate tasks
                }
            }
        }
    }


    public void remove(OneTimeTicker oneTimeTicker) {
        synchronized (locker) {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                if (tasks.get(i).oneTimeTicker == oneTimeTicker) {
                    tasks.get(i).killMe();
                    // break; Continue loop because there may be more duplicate tasks
                }
            }
        }
    }

    public void removeAll() {
        synchronized (locker) {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                tasks.get(i).killMe();
            }
        }
    }

    public void removeAllByTag(String tag) {
        synchronized (locker) {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                if (tasks.get(i).tag.equals(tag)) {
                    tasks.get(i).killMe();
                }
            }
        }
    }
}

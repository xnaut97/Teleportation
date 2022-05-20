package io.github.tezvn.teleportation.utils;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class AbstractThread {

    private final JavaPlugin plugin;

    private boolean running;

    protected BukkitRunnable runnable;

    private boolean async;

    protected ThreadType type;

    protected int id;

    public AbstractThread(JavaPlugin plugin, boolean async, ThreadType type) {
        this.plugin = plugin;
        this.async = async;
        this.type = type;
    }

    public abstract void onTick();

    public void onStop() {}

    public void onStart() {}

    protected JavaPlugin getPlugin() {
        return this.plugin;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isRunning() {
        return running;
    }

    public ThreadType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    protected void setRunning(boolean running) {
        this.running = running;
    }

    protected boolean isCurrentlyRunning() {
        return Bukkit.getScheduler().isCurrentlyRunning(getId());
    }

    public abstract void start();

    public void stop() {
        if(!isRunning())
            return;
        setRunning(false);
    }

    protected void init() {
        if(isCurrentlyRunning())
            return;
        this.setRunning(true);
        this.runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if(!isRunning()) {
                    cancel();
                    onStop();
                    return;
                }
                onTick();
            }
        };
    }

    public abstract static class BaseThread extends AbstractThread {

        public BaseThread(JavaPlugin plugin, boolean async) {
            super(plugin, async, ThreadType.BASE);
        }

        @Override
        public final void start() {
            init();
            onStart();
            if(isAsync())
                this.runnable.runTask(getPlugin());
            else
                this.runnable.runTaskAsynchronously(getPlugin());
        }

    }

    public abstract static class DelayedThread extends AbstractThread {

        private long ticks;

        public DelayedThread(JavaPlugin plugin, boolean async, long ticks) {
            super(plugin, async, ThreadType.DELAYED);
            this.ticks = ticks;
        }

        @Override
        public final void start() {
            if(isCurrentlyRunning())
                return;
            this.setRunning(true);
            init();
            onStart();
            if(isAsync())
                this.runnable.runTaskLaterAsynchronously(getPlugin(), ticks);
            else
                this.runnable.runTaskLater(getPlugin(), ticks);
        }
    }

    public abstract static class TimerThread extends AbstractThread {

        private long ticks;

        private long delay;

        private boolean pause;

        public TimerThread(JavaPlugin plugin, boolean async, long delay, long ticks) {
            super(plugin, async, ThreadType.TIMER);
            this.delay = delay;
            this.ticks = ticks;
        }

        @Override
        protected void init() {
            this.runnable = new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if(!isRunning()) {
                        cancel();
                        onStop();
                        return;
                    }
                    if(isPaused())
                        return;
                    onTick();
                    count++;
                }
            };
        }

        @Override
        public final void start() {
            if(isCurrentlyRunning())
                return;
            this.setRunning(true);
            init();
            onStart();
            if(isAsync())
                this.runnable.runTaskTimer(getPlugin(), delay, ticks);
            else
                this.runnable.runTaskTimerAsynchronously(getPlugin(), delay, ticks);
        }

        public long getTicks() {
            return ticks;
        }

        public long getDelay() {
            return delay;
        }

        public boolean isPaused() {
            return pause;
        }

        public void pause() {
            this.pause = true;
            onPause();
        }

        public void resume() {
            this.pause = false;
            onResume();
        }

        public void onPause() {}

        public void onResume() {}
    }

    public abstract static class TimedThread extends TimerThread {

        private final long times;

        private int currentTimes;

        public TimedThread(JavaPlugin plugin, boolean async, long delay, long ticks, long times) {
            super(plugin, async, delay, ticks);
            this.times = times;
            this.type = ThreadType.TIMED;
        }

        public long getTimes() {
            return times;
        }

        public int getCurrentTimes() {
            return currentTimes;
        }

        public void onSuccess() {}

        @Override
        protected void init() {
            this.runnable = new BukkitRunnable() {
                int count = 0;
                @Override
                public void run() {
                    if(!isRunning()) {
                        cancel();
                        onStop();
                        return;
                    }
                    if(count >= getTimes()){
                        cancel();
                        onSuccess();
                        return;
                    }
                    if(isPaused())
                        return;
                    onTick();
                    count++;
                    currentTimes = count;
                }
            };
        }

    }

    public enum ThreadType {
        BASE,
        DELAYED,
        TIMER,
        TIMED;
    }
}

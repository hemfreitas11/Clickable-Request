package me.bkrmt.bkcore.request;

@FunctionalInterface
public interface ExpireRunnable {
    void run(ClickableRequest request);
}

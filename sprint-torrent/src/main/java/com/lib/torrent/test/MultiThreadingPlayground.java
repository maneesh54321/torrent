package com.lib.torrent.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultiThreadingPlayground {

  public static void main(String[] args) {
    ExecutorService executorService = Executors.newFixedThreadPool(2);
    List<Future<String>> futureList = new ArrayList<>();
    try {

      futureList.add(executorService.submit( () -> {
        try {
          Thread.sleep(3500);
          System.out.println(Thread.currentThread().getName()+ ": Failing!!");
          throw new Exception("Some exception");
        } catch (InterruptedException e) {
          System.out.println("Thread interrupted!! " + Thread.currentThread().getName());
          throw e;
        } catch (Exception e) {
          futureList.forEach(future -> future.cancel(false));
          throw new ExecutionException(e);
        }
      }));

      for (int i = 0; i < 4; i++) {
        futureList.add(executorService.submit(() -> {
          try {
            Thread.sleep(5000);
            System.out.println(Thread.currentThread().getName()+": Completed!!");
            return "Completed";
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }));
      }

      for (Future<String> future :
          futureList) {
        System.out.println(future.get());
      }

    } catch (ExecutionException e) {
//      System.out.println(e.getMessage());
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      executorService.shutdown();
    }
  }

}

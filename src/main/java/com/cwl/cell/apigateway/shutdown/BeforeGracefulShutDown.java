package com.cwl.cell.apigateway.shutdown;

import com.cwl.cell.apigateway.config.WebServerConfiguration;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@DependsOn("webServerGracefulShutdown")
public class BeforeGracefulShutDown implements SmartLifecycle {

  public final WebServerConfiguration webServerConfiguration;

  //private final WebServerManager serverManager;

  private volatile boolean running;

/*
  BeforeGracefulShutDown(WebServerManager serverManager) {
    this.serverManager = serverManager;
  }
*/

  @Override
  public void start() {
    this.running = true;
  }

  @Override
  public void stop() {
    throw new UnsupportedOperationException("Stop must not be invoked directly");
  }

  @Override
  public void stop(Runnable callback) {
    webServerConfiguration.stop();
    this.running = false;
    callback.run();
  }

  @Override
  public boolean isRunning() {
    return this.running;
  }

}

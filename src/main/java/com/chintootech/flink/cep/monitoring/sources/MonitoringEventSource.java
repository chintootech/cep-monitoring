/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chintootech.flink.cep.monitoring.sources;

import com.chintootech.flink.cep.monitoring.events.MonitoringEvent;
import com.chintootech.flink.cep.monitoring.events.TemperatureEvent;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import com.chintootech.flink.cep.monitoring.events.PowerEvent;

import java.util.concurrent.ThreadLocalRandom;

public class MonitoringEventSource extends RichParallelSourceFunction<MonitoringEvent> {

    private boolean running = true;

    private final int maxRackId;

    private final long pause;

    private final double temperatureRatio;

    private final double powerStd;

    private final double powerMean;

    private final double temperatureStd;

    private final double temperatureMean;

    private int shard;

    private int offset;

    public MonitoringEventSource(
            int maxRackId,
            long pause,
            double temperatureRatio,
            double powerStd,
            double powerMean,
            double temperatureStd,
            double temperatureMean) {
        this.maxRackId = maxRackId;
        this.pause = pause;
        this.temperatureRatio = temperatureRatio;
        this.powerMean = powerMean;
        this.powerStd = powerStd;
        this.temperatureMean = temperatureMean;
        this.temperatureStd = temperatureStd;
    }

    @Override
    public void open(Configuration configuration) {
        int numberTasks = getRuntimeContext().getNumberOfParallelSubtasks();
        int index = getRuntimeContext().getIndexOfThisSubtask();

        offset = (int)((double)maxRackId / numberTasks * index);
        shard = (int)((double)maxRackId / numberTasks * (index + 1)) - offset;
    }

    public void run(SourceContext<MonitoringEvent> sourceContext) throws Exception {
        while (running) {
            MonitoringEvent monitoringEvent;

            final ThreadLocalRandom random = ThreadLocalRandom.current();

            if (shard > 0) {
                int rackId = random.nextInt(shard) + offset;

                if (random.nextDouble() >= temperatureRatio) {
                    double power = random.nextGaussian() * powerStd + powerMean;
                    monitoringEvent = new PowerEvent(rackId, power);
                } else {
                    double temperature = random.nextGaussian() * temperatureStd + temperatureMean;
                    monitoringEvent = new TemperatureEvent(rackId, temperature);
                }


                sourceContext.collect(monitoringEvent);
            }

            Thread.sleep(pause);
        }
    }

    public void cancel() {
        running = false;
    }
}

/**
 * Licensed to the Camel Labs under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.camellabs.component.raspberrypi.i2c;

import java.io.IOException;

import com.pi4j.io.i2c.I2CDevice;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The I2C producer.
 */
public class I2CProducer extends DefaultProducer implements I2CDevice {
    private static final Logger LOG = LoggerFactory.getLogger(I2CProducer.class);
    private I2CEndpoint endpoint;
    private I2CDevice device;

    public I2CProducer(I2CEndpoint endpoint, I2CDevice device) {
        super(endpoint);
        this.endpoint = endpoint;
        this.device = device;
    }

    /**
     * 
     */
    public void process(Exchange exchange) throws Exception {
        Object o = exchange.getIn().getBody();

        exchange.isTransacted();

        if (o instanceof Byte) {
            if (endpoint.getAddress() >= 0) {
                write(endpoint.getAddress(), (Byte)o);
            } else {
                write((Byte)o);
            }
        } else if (o instanceof Byte[]) {
            if (endpoint.getAddress() >= 0) {
                write(endpoint.getAddress(), (byte[])o, endpoint.getOffset(), endpoint.getSize());
            } else {
                write((byte[])o, endpoint.getOffset(), endpoint.getSize());
            }
        }

    }

    public int read() throws IOException {
        return this.device.read();
    }

    public int read(byte[] buffer, int offset, int size) throws IOException {
        return this.device.read(buffer, offset, size);
    }

    public int read(byte[] writeBuffer, int writeOffset, int writeSize, byte[] buffer, int offset, int size) throws IOException {
        return this.device.read(writeBuffer, writeOffset, writeSize, buffer, offset, size);
    }

    public int read(int address) throws IOException {
        return this.device.read(address);
    }

    public int read(int address, byte[] buffer, int offset, int size) throws IOException {
        return this.device.read(address, buffer, offset, size);
    }

    public void write(byte b) throws IOException {
        this.device.write(b);
    }

    public void write(byte[] buffer, int offset, int size) throws IOException {
        this.device.write(buffer, offset, size);
    }

    public void write(int address, byte b) throws IOException {
        this.device.write(address, b);
    }

    public void write(int address, byte[] buffer, int offset, int size) throws IOException {
        this.device.read(address, buffer, offset, size);
    }
}
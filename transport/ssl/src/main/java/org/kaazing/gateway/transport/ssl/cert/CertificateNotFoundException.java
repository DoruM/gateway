/**
 * Copyright 2007-2016, Kaazing Corporation. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kaazing.gateway.transport.ssl.cert;

/*
 * This exception is thrown when there are no certificates available 
 * for a specific host bound to a host:port. 
 */
public class CertificateNotFoundException
    extends CertificateBindingException {

    private static final long serialVersionUID = 3916530339075894179L;

    public CertificateNotFoundException() {
        super();
    }

    public CertificateNotFoundException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public CertificateNotFoundException(Throwable cause) {
        super(cause);
    }

    public CertificateNotFoundException(String msg) {
        super(msg);
    }
}

/*
 *
 * Copyright (c) 2019 Lijun Liao
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

package demo.pkcs.pkcs11.wrapper.speed.encryption;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import demo.pkcs.pkcs11.wrapper.TestBase;
import demo.pkcs.pkcs11.wrapper.speed.ConcurrentSessionBagEntry;
import demo.pkcs.pkcs11.wrapper.speed.Pkcs11Executor;
import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.ValuedSecretKey;

/**
 * Decryptor executor base class.
 *
 * @author Lijun Liao
 */
public abstract class DecryptExecutor extends Pkcs11Executor {

  private static final Logger LOG =
      LoggerFactory.getLogger(DecryptExecutor.class);

  public class MyRunnable implements Runnable {

    public MyRunnable() {
    }

    private byte[] out = new byte[plainData.length + 64];

    @Override
    public void run() {
      while (!stop()) {
        try {
          ConcurrentSessionBagEntry sessionBag = borrowSession();
          try {
            Session session = sessionBag.value();
            // initialize for signing
            session.decryptInit(encryptMechanism,key);
            // This signing operation is implemented in most of the drivers
            int len = session.decrypt(dataToDecrypt, 0, dataToDecrypt.length,
                out, 0, out.length);
            byte[] decryptedData = Arrays.copyOf(out, len);
            Assert.assertArrayEquals(plainData, decryptedData);
          } finally {
            requiteSession(sessionBag);
          }

          account(1, 0);
        } catch (Throwable th) {
          System.err.println(th.getMessage());
          LOG.error("error", th);
          account(1, 1);
        }
      }
    }

  }

  private final Mechanism encryptMechanism;

  private final byte[] dataToDecrypt;

  private final byte[] plainData;

  private ValuedSecretKey key;

  protected abstract ValuedSecretKey getMinimalKeyTemplate();

  public DecryptExecutor(String description, Mechanism keyGenMechanism,
      Token token, char[] pin, Mechanism encryptMechanism, int inputLen)
          throws TokenException {
    super(description, token, pin);
    this.encryptMechanism = encryptMechanism;
    this.plainData = TestBase.randomBytes(inputLen);

    // generate keypair on token
    ValuedSecretKey keyTemplate = getMinimalKeyTemplate();
    keyTemplate.getSensitive().setBooleanValue(Boolean.TRUE);
    keyTemplate.getToken().setBooleanValue(Boolean.TRUE);
    byte[] id = new byte[20];
    new Random().nextBytes(id);
    keyTemplate.getId().setByteArrayValue(id);

    keyTemplate.getEncrypt().setBooleanValue(Boolean.TRUE);
    keyTemplate.getDecrypt().setBooleanValue(Boolean.TRUE);

    ConcurrentSessionBagEntry sessionBag = borrowSession();
    try {
      Session session = sessionBag.value();
      key = (ValuedSecretKey) session.generateKey(keyGenMechanism, keyTemplate);

      session.encryptInit(encryptMechanism, key);
      byte[] buffer = new byte[inputLen + 64];
      int len = session.encrypt(plainData, 0, inputLen,
                  buffer, 0, buffer.length);
      this.dataToDecrypt = Arrays.copyOf(buffer, len);
    } finally {
      requiteSession(sessionBag);
    }

  }

  @Override
  protected Runnable getTestor() throws Exception {
    return new MyRunnable();
  }

  @Override
  public void close() {
    if (key != null) {
      ConcurrentSessionBagEntry sessionBag = borrowSession();
      try {
        Session session = sessionBag.value();
        session.destroyObject(key);
      } catch (Throwable th) {
        LOG.error("could not destroy generated objects", th);
      } finally {
        requiteSession(sessionBag);
      }
    }

    super.close();
  }

}

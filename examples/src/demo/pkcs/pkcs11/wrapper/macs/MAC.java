// Copyright (c) 2002 Graz University of Technology. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution.
//
// 3. The end-user documentation included with the redistribution, if any, must
//    include the following acknowledgment:
//
//    "This product includes software developed by IAIK of Graz University of
//     Technology."
//
//    Alternately, this acknowledgment may appear in the software itself, if and
//    wherever such third-party acknowledgments normally appear.
//
// 4. The names "Graz University of Technology" and "IAIK of Graz University of
//    Technology" must not be used to endorse or promote products derived from
//    this software without prior written permission.
//
// 5. Products derived from this software may not be called "IAIK PKCS Wrapper",
//    nor may "IAIK" appear in their name, without prior written permission of
//    Graz University of Technology.
//
// THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED
// WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE LICENSOR BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
// OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
// OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
// ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package demo.pkcs.pkcs11.wrapper.macs;

import java.math.BigInteger;
import java.security.SecureRandom;

import org.junit.Test;

import demo.pkcs.pkcs11.wrapper.TestBase;
import demo.pkcs.pkcs11.wrapper.util.Util;
import iaik.pkcs.pkcs11.Mechanism;
import iaik.pkcs.pkcs11.Session;
import iaik.pkcs.pkcs11.Token;
import iaik.pkcs.pkcs11.TokenException;
import iaik.pkcs.pkcs11.objects.ValuedSecretKey;
import iaik.pkcs.pkcs11.wrapper.PKCS11Constants;

/**
 * This demo program uses a PKCS#11 module to MAC a given file and test if the
 * MAC can be verified.
 */
public class MAC extends TestBase {

  @Test
  public void main() throws TokenException {
    Token token = getNonNullToken();
    Session session = openReadOnlySession(token);
    try {
      main0(token, session);
    } finally {
      session.closeSession();
    }
  }

  private void main0(Token token, Session session) throws TokenException {
    LOG.info("##################################################");
    LOG.info("generate secret MAC key");

    ValuedSecretKey macKeyTemplate = ValuedSecretKey.newGenericSecretKey();
    macKeyTemplate.getSign().setBooleanValue(Boolean.TRUE);
    macKeyTemplate.getVerify().setBooleanValue(Boolean.TRUE);
    macKeyTemplate.getToken().setBooleanValue(Boolean.FALSE);

    ValuedSecretKey secretMACKey;
    int keyBytesLen = 32;
    Mechanism keyMechanism =
        Mechanism.get(PKCS11Constants.CKM_GENERIC_SECRET_KEY_GEN);
    if (Util.supports(token, keyMechanism.getMechanismCode())) {
      LOG.info("generate secret MAC key");
      macKeyTemplate.getValueLen().setLongValue(Long.valueOf(keyBytesLen));
      secretMACKey = (ValuedSecretKey)
          session.generateKey(keyMechanism, macKeyTemplate);
    } else {
      LOG.info("import secret MAC key (generation not supported)");
      byte[] keyValue = new byte[keyBytesLen];
      new SecureRandom().nextBytes(keyValue);
      macKeyTemplate.getValue().setByteArrayValue(keyValue);

      secretMACKey = (ValuedSecretKey) session.createObject(macKeyTemplate);
    }

    LOG.info("##################################################");
    Mechanism signatureMechanism = getSupportedMechanism(token,
        PKCS11Constants.CKM_SHA256_HMAC);
    byte[] rawData = randomBytes(1057);

    session.signInit(signatureMechanism, secretMACKey);
    byte[] macValue = session.sign(rawData);
    LOG.info("The MAC value is: {}", new BigInteger(1, macValue).toString(16));

    LOG.info("##################################################");
    LOG.info("verification of the MAC... ");

    // initialize for verification
    session.verifyInit(signatureMechanism, secretMACKey);
    // throws an exception upon unsuccessful verification
    session.verify(rawData, macValue);

    LOG.info("##################################################");
  }

}

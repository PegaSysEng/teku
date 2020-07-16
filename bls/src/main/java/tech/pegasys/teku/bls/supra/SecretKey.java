package tech.pegasys.teku.bls.supra;

import static com.google.common.base.Preconditions.checkArgument;

import java.security.SecureRandom;
import org.apache.tuweni.bytes.Bytes;
import tech.pegasys.teku.bls.supra.swig.blst;
import tech.pegasys.teku.bls.supra.swig.p1;
import tech.pegasys.teku.bls.supra.swig.p1_affine;
import tech.pegasys.teku.bls.supra.swig.scalar;

public class SecretKey {

  public static SecretKey fromBytes(Bytes bytes) {
    checkArgument(bytes.size() == 32, "Expected 32 bytes but received %s.", bytes.size());
    scalar scalarVal = new scalar();
    blst.scalar_from_bendian(scalarVal, bytes.toArrayUnsafe());
    return new SecretKey(scalarVal);
  }

  public static SecretKey generateNew(SecureRandom random) {
    byte[] ikm = new byte[128];
    random.nextBytes(ikm);
    scalar scalar = new scalar();
    blst.keygen(scalar, ikm, null);
    return new SecretKey(scalar);
  }

  public final scalar scalarVal;

  public SecretKey(scalar scalarVal) {
    this.scalarVal = scalarVal;
  }

  public Bytes toBytes() {
    byte[] res = new byte[32];
    blst.bendian_from_scalar(res, scalarVal);
    return Bytes.wrap(res);
  }

  public PublicKey toPublicKey() {
    p1 p1 = new p1();
    blst.sk_to_pk_in_g1(p1, scalarVal);
    p1_affine p1_affine = new p1_affine();
    blst.p1_to_affine(p1_affine, p1);
    p1.delete();
    return new PublicKey(p1_affine);
  }
}

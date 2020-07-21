package tech.pegasys.teku.bls.impl.blst;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.apache.tuweni.bytes.Bytes;
import tech.pegasys.teku.bls.BatchSemiAggregate;
import tech.pegasys.teku.bls.impl.BLS12381;
import tech.pegasys.teku.bls.impl.KeyPair;
import tech.pegasys.teku.bls.impl.PublicKey;
import tech.pegasys.teku.bls.impl.Signature;
import tech.pegasys.teku.bls.impl.blst.swig.BLST_ERROR;
import tech.pegasys.teku.bls.impl.blst.swig.blst;
import tech.pegasys.teku.bls.impl.blst.swig.p2;
import tech.pegasys.teku.bls.impl.blst.swig.p2_affine;
import tech.pegasys.teku.bls.impl.blst.swig.pairing;

public class BlstBLS12381 implements BLS12381 {

  public static BlstBLS12381 INSTANCE = new BlstBLS12381();

  private static final int BATCH_RANDOM_BYTES = 8;

  static {
    try {
      NativeUtils.loadLibraryFromJar("/" + System.mapLibraryName("jblst"));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Random getRND() {
    // Milagro RAND has some issues with generating 'small' random numbers
    // and is not thread safe
    // Using non-secure random due to the JDK Linux secure random issue:
    // https://bugs.java.com/bugdatabase/view_bug.do?bug_id=6521844
    // A potential attack here has a very limited application and is not feasible
    // Thus using non-secure random doesn't significantly mitigate the security
    return ThreadLocalRandom.current();
  }

  public static BlstSignature sign(BlstSecretKey secretKey, Bytes message) {
    p2 p2Signature = new p2();
    p2 hash = HashToCurve.hashToG2(message);
    blst.sign_pk_in_g1(p2Signature, hash, secretKey.scalarVal);
    p2_affine p2SignatureAffine = new p2_affine();
    blst.p2_to_affine(p2SignatureAffine, p2Signature);
    p2Signature.delete();
    hash.delete();
    return new BlstSignature(p2SignatureAffine);
  }

  public static boolean verify(BlstPublicKey publicKey, Bytes message, BlstSignature signature) {
    BLST_ERROR res = blst.core_verify_pk_in_g1(
        publicKey.ecPoint,
        signature.ec2Point,
        1,
        message.toArrayUnsafe(),
        HashToCurve.ETH2_DST.toArrayUnsafe(),
        new byte[0]);
    return res == BLST_ERROR.BLST_SUCCESS;
  }

  @Override
  public KeyPair generateKeyPair(Random random) {
    BlstSecretKey secretKey = BlstSecretKey.generateNew(random);
    return new KeyPair(secretKey);
  }

  @Override
  public BlstPublicKey publicKeyFromCompressed(Bytes compressedPublicKeyBytes) {
    return BlstPublicKey.fromBytes(compressedPublicKeyBytes);
  }

  @Override
  public BlstSignature signatureFromCompressed(Bytes compressedSignatureBytes) {
    return BlstSignature.fromBytes(compressedSignatureBytes);
  }

  @Override
  public BlstSecretKey secretKeyFromBytes(Bytes secretKeyBytes) {
    return BlstSecretKey.fromBytes(secretKeyBytes);
  }

  @Override
  public BlstPublicKey aggregatePublicKeys(List<? extends PublicKey> publicKeys) {
    return BlstPublicKey.aggregate(
        publicKeys.stream().map(k -> (BlstPublicKey) k).collect(Collectors.toList()));
  }

  @Override
  public BlstSignature aggregateSignatures(List<? extends Signature> signatures) {
    return BlstSignature.aggregate(
        signatures.stream().map(s -> (BlstSignature) s).collect(Collectors.toList()));
  }

  @Override
  public BlstBatchSemiAggregate prepareBatchVerify(int index, List<? extends PublicKey> publicKeys,
      Bytes message, Signature signature) {
    BlstPublicKey aggrPubKey = aggregatePublicKeys(publicKeys);
    p2 p2 = HashToCurve.hashToG2(message);
    p2_affine p2Affine = new p2_affine();
    blst.p2_to_affine(p2Affine, p2);

    pairing ctx = new pairing();
    try {
      blst.pairing_init(ctx);
      BLST_ERROR ret = blst.pairing_mul_n_aggregate_pk_in_g1(
          ctx,
          aggrPubKey.ecPoint,
          ((BlstSignature)signature).ec2Point,
          p2Affine,
          nextBatchRandomMultiplier(),
          BATCH_RANDOM_BYTES * 8);
      if (ret != BLST_ERROR.BLST_SUCCESS) throw new IllegalArgumentException("Error: " + ret);
    } catch (Exception e) {
      ctx.delete();
      throw e;
    } finally {
      p2.delete();
      p2Affine.delete(); // not sure if its copied inside pairing_mul_n_aggregate_pk_in_g1
    }
    blst.pairing_commit(ctx);

    return new BlstBatchSemiAggregate(ctx);
  }

  @Override
  public BlstBatchSemiAggregate prepareBatchVerify2(int index, List<? extends PublicKey> publicKeys1,
      Bytes message1, Signature signature1, List<? extends PublicKey> publicKeys2, Bytes message2,
      Signature signature2) {
    BlstBatchSemiAggregate aggregate1 =
        prepareBatchVerify(index, publicKeys1, message1, signature1);
    BlstBatchSemiAggregate aggregate2 =
        prepareBatchVerify(index + 1, publicKeys2, message2, signature2);
    aggregate1.mergeWith(aggregate2);
    aggregate2.release();

    return aggregate1;
  }

  @Override
  public boolean completeBatchVerify(List<? extends BatchSemiAggregate> preparedList) {
    List<BlstBatchSemiAggregate> blstList =
        preparedList.stream().map(b -> (BlstBatchSemiAggregate) b).collect(Collectors.toList());

    if (blstList.isEmpty()) {
      return true;
    }
    pairing ctx0 = blstList.get(0).getCtx();
    boolean mergeRes = true;
    for (int i = 1; i < blstList.size(); i++) {
      BLST_ERROR ret = blst.pairing_merge(ctx0, blstList.get(i).getCtx());
      mergeRes &= ret == BLST_ERROR.BLST_SUCCESS;
      blstList.get(i).release();
    }

    int boolRes = blst.pairing_finalverify(ctx0, null);
    blstList.get(0).release();
    return mergeRes && boolRes != 0;
  }

  private static BigInteger nextBatchRandomMultiplier() {
    byte[] scalarBytes = new byte[BATCH_RANDOM_BYTES];
    getRND().nextBytes(scalarBytes);
    return new BigInteger(1, scalarBytes);
  }
}

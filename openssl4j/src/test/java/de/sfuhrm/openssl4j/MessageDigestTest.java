package de.sfuhrm.openssl4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
/**
 * Test cases that compare the message digest of the
 * Sun provider (aka 'reference') with the implementations
 * in this context (aka 'test').
 * @author Stephan Fuhrmann
 */
public class MessageDigestTest {

    private Formatter formatter;
    private Charset ascii;

    @BeforeEach
    public void init() {
        formatter = Formatter.getInstance();
        ascii = StandardCharsets.US_ASCII;
    }

    private static Stream<Arguments> provideTestArguments() throws NoSuchAlgorithmException, IOException {
        List<String> messageDigestNames = Arrays.asList("MD5", "SHA1", "SHA-224", "SHA-256", "SHA-384", "SHA-512", "SHA-512/224", "SHA-512/256", "SHA3-224", "SHA3-256", "SHA3-384", "SHA3-512");
        List<Arguments> result = new ArrayList<>();
        Provider openSsl = new OpenSSLProvider();
        Provider sun = MessageDigest.getInstance("MD5").getProvider();

            for (String messageDigestName : messageDigestNames) {
                    result.add(Arguments.of(
                            messageDigestName,
                            MessageDigest.getInstance(messageDigestName, openSsl),
                            MessageDigest.getInstance(messageDigestName, sun)));
            }

        return result.stream();
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void compareGetters(String digestName, MessageDigest testMD, MessageDigest referenceMD) {
        assertEquals(referenceMD.getAlgorithm(), testMD.getAlgorithm());
        assertEquals(referenceMD.getDigestLength(), testMD.getDigestLength());
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void digestWithNoData(String digestName, MessageDigest testMD, MessageDigest referenceMD) {
        byte[] actualDigest = testMD.digest();
        byte[] expectedDigest = referenceMD.digest();

        assertEquals(expectedDigest.length, actualDigest.length);
        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    private byte[] franzJagt() {
        byte[] data = "Franz jagt im komplett verwahrlosten Taxi quer durch Bayern".getBytes(ascii);
        return data;
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void digestWithStringBytes(String digestName, MessageDigest testMD, MessageDigest referenceMD) {

        byte[] actualDigest = testMD.digest(franzJagt());
        byte[] expectedDigest = referenceMD.digest(franzJagt());

        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithFullArray(String digestName, MessageDigest testMD, MessageDigest referenceMD) {
        testMD.update(franzJagt());
        byte[] actualDigest = testMD.digest();

        byte[] expectedDigest = referenceMD.digest(franzJagt());

        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithSingleBytes(String digestName, MessageDigest testMD, MessageDigest referenceMD) {
        for (byte val : franzJagt()) {
            testMD.update(val);
        }
        byte[] actualDigest = testMD.digest();

        for (byte val : franzJagt()) {
            referenceMD.update(val);
        }
        byte[] expectedDigest = referenceMD.digest();
        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithHeapByteBuffer(String digestName, MessageDigest testMD, MessageDigest referenceMD) {

        ByteBuffer bb = ByteBuffer.wrap(franzJagt());

        ByteBuffer actualCopy = bb.duplicate();
        testMD.update(actualCopy);
        byte[] actualDigest = testMD.digest();

        ByteBuffer expectedCopy = bb.duplicate();
        referenceMD.update(expectedCopy);

        byte[] expectedDigest = referenceMD.digest();
        assertEquals(expectedCopy.position(), actualCopy.position());
        assertEquals(expectedCopy.limit(), actualCopy.limit());
        assertEquals(expectedCopy.capacity(), actualCopy.capacity());
        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithDirectByteBufferNoRemaining(String digestName, MessageDigest testMD, MessageDigest referenceMD) {
        ByteBuffer bb = ByteBuffer.allocateDirect(franzJagt().length);
        bb.put(franzJagt());

        ByteBuffer actualCopy = bb.duplicate();
        testMD.update(actualCopy);
        byte[] actualDigest = testMD.digest();

        ByteBuffer expectedCopy = bb.duplicate();
        referenceMD.update(expectedCopy);

        byte[] expectedDigest = referenceMD.digest();
        assertEquals(expectedCopy.position(), actualCopy.position());
        assertEquals(expectedCopy.limit(), actualCopy.limit());
        assertEquals(expectedCopy.capacity(), actualCopy.capacity());
        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithDirectByteBuffer(String digestName, MessageDigest testMD, MessageDigest referenceMD) {
        ByteBuffer bb = ByteBuffer.allocateDirect(franzJagt().length);
        bb.put(franzJagt());
        bb.flip();

        ByteBuffer actualCopy = bb.duplicate();
        testMD.update(actualCopy);
        byte[] actualDigest = testMD.digest();

        ByteBuffer expectedCopy = bb.duplicate();
        referenceMD.update(expectedCopy);

        byte[] expectedDigest = referenceMD.digest();
        assertEquals(expectedCopy.position(), actualCopy.position());
        assertEquals(expectedCopy.limit(), actualCopy.limit());
        assertEquals(expectedCopy.capacity(), actualCopy.capacity());
        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithFragmentedArray(String digestName, MessageDigest testMD, MessageDigest referenceMD) {
        byte[] dataInner = franzJagt();
        byte[] data = Arrays.copyOf(dataInner, dataInner.length * 2);
        testMD.update(data, 0, dataInner.length);
        byte[] actualDigest = testMD.digest();

        referenceMD.update(data, 0, dataInner.length);
        byte[] expectedDigest = referenceMD.digest();

        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithLongArray(String digestName, MessageDigest testMD, MessageDigest referenceMD) {

        byte[] data = new byte[1024*1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)i;
        }
        int rounds = 16;

        for (int i=0; i < rounds; i++) {
            testMD.update(data, 0, data.length);
        }
        byte[] actualDigest = testMD.digest();

        for (int i=0; i < rounds; i++) {
            referenceMD.update(data, 0, data.length);
        }
        byte[] expectedDigest = referenceMD.digest();

        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }

    @ParameterizedTest
    @MethodSource("provideTestArguments")
    public void updateWithLongDirectBB(String digestName, MessageDigest testMD, MessageDigest referenceMD) {

        byte[] data = new byte[1024*1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)i;
        }
        int rounds = 16;
        ByteBuffer direct = ByteBuffer.allocateDirect(data.length);
        direct.put(data);
        direct.flip();

        for (int i=0; i < rounds; i++) {
            testMD.update(direct);
            direct.flip();
        }
        byte[] actualDigest = testMD.digest();

        for (int i=0; i < rounds; i++) {
            referenceMD.update(direct);
            direct.flip();
        }
        byte[] expectedDigest = referenceMD.digest();

        assertEquals(formatter.format(expectedDigest), formatter.format(actualDigest));
    }
}

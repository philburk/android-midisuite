package com.mobileer.miditools.midi20;

import com.mobileer.miditools.midi20.inquiry.CapabilityNegotiator;
import com.mobileer.miditools.midi20.inquiry.InquiryMessage;
import com.mobileer.miditools.midi20.inquiry.ProtocolTypeMidi20;
import com.mobileer.miditools.midi20.protocol.SysExDecoder;
import com.mobileer.miditools.midi20.protocol.SysExEncoder;
import com.mobileer.miditools.midi20.protocol.UniversalMidiPacket;
import com.mobileer.miditools.midi20.protocol.PacketDecoder;
import com.mobileer.miditools.midi20.protocol.PacketEncoder;
import com.mobileer.miditools.midi20.protocol.RawByteDecoder;
import com.mobileer.miditools.midi20.protocol.RawByteEncoder;
import com.mobileer.miditools.midi20.tools.Midi;
import com.mobileer.miditools.midi20.tools.MidiReader;
import com.mobileer.miditools.midi20.tools.MidiWriter;
import com.mobileer.miditools.midi20.tools.SysExParser;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

import androidx.annotation.NonNull;

/**
 * Local unit test for Packet.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    // Test type, opcode, group, channel
    @Test
    public void packetBasics() {
        UniversalMidiPacket packet = UniversalMidiPacket.create();
        assertEquals(0, packet.getType());
        assertEquals(0, packet.getOpcode());
        assertEquals(0, packet.getGroup());
        assertEquals(0, packet.getChannel());
        packet.setType(5);
        packet.setOpcode(7);
        packet.setGroup(11);
        packet.setChannel(13);
        assertEquals(5, packet.getType());
        assertEquals(7, packet.getOpcode());
        assertEquals(11, packet.getGroup());
        assertEquals(13, packet.getChannel());
    }

    // Test NoteOn and NoteOff encoding
    @Test
    public void testBasicNoteOn() {
        UniversalMidiPacket packet = UniversalMidiPacket.create();
        int noteNumber = 62;
        int velocity = 29876;
        packet.noteOn(noteNumber, velocity);
        packet.setChannel(0);
        assertEquals(noteNumber, packet.getNoteNumber());
        assertEquals(velocity, packet.getVelocity());

        velocity = 18542;
        packet.noteOff(noteNumber, velocity);
        assertEquals(noteNumber, packet.getNoteNumber());
        assertEquals(velocity, packet.getVelocity());
    }

    @Test
    public void testBasicControlChange() {
        UniversalMidiPacket packet = UniversalMidiPacket.create();
        int index = 62;
        int channel = 7;
        long value = 0x012345678;

        packet.controlChange(index, value);
        packet.setChannel(channel);
        assertTrue(packet.getOpcode() == UniversalMidiPacket.OPCODE_CONTROL_CHANGE);
        assertEquals(index, packet.getControllerIndex());
        assertEquals(value, packet.getControllerValue());
        assertEquals(channel, packet.getChannel());

        packet.setNormalizedControllerValue(0.25);
        assertEquals(0x040000000L, packet.getControllerValue());
        packet.setNormalizedControllerValue(0.5);
        assertEquals(0x080000000L, packet.getControllerValue());
        packet.setNormalizedControllerValue(0.75);
        assertEquals(0x0C0000000L, packet.getControllerValue());

        double dvalue = 0.1;
        for (dvalue = 0.0; dvalue < 1.0; dvalue += 0.0678) {
            packet.setNormalizedControllerValue(dvalue);
            assertEquals(dvalue, packet.getNormalizedControllerValue(), 0.001);
        }

        index = 94;
        dvalue = 0.254321;
        packet.controlChange(index, dvalue);

        assertTrue(packet.getOpcode() == UniversalMidiPacket.OPCODE_CONTROL_CHANGE);
        assertEquals(index, packet.getControllerIndex());
        assertEquals(dvalue, packet.getNormalizedControllerValue(), 0.001);
    }

    @Test
    public void testProgramChange() {
        UniversalMidiPacket packet = new UniversalMidiPacket();
        int program = 0x37;
        int bank = 0x1234;
        int channel = 0xE;

        packet.programChange(program, bank);
        packet.setChannel(channel);
        assertEquals(program, packet.getProgram());
        assertEquals(bank, packet.getBank());
        assertEquals(channel, packet.getChannel());

        // Check raw packet bits based on spec.
        assertEquals(packet.getWord(0), 0x40CE0001);
        assertEquals(packet.getWord(1), 0x37002434);
    }

    @Test
    public void testRPN() {
        UniversalMidiPacket packet = new UniversalMidiPacket();
        int index = (55 << 7) + 93;
        int channel = 14;
        long value = 0x098765432L;

        packet.RPN(index, value);
        packet.setChannel(channel);
        assertTrue(packet.getOpcode() == UniversalMidiPacket.OPCODE_RPN);
        assertEquals(index, packet.getControllerIndex());
        assertEquals(value, packet.getControllerValue());
        assertEquals(channel, packet.getChannel());
    }

    @Test
    public void testNRPN() {
        UniversalMidiPacket packet = new UniversalMidiPacket();
        int index = (95 << 7) + 27;
        int channel = 5;
        long value = 0x056473829L;

        packet.NRPN(index, value);
        packet.setChannel(channel);
        assertTrue(packet.getOpcode() == UniversalMidiPacket.OPCODE_NRPN);
        assertEquals(index, packet.getControllerIndex());
        assertEquals(value, packet.getControllerValue());
        assertEquals(channel, packet.getChannel());
    }

    @Test
    public void testSysEx7() {
        UniversalMidiPacket packet = new UniversalMidiPacket();
        byte[] payload = new byte[]{ 0x00, 0x00, 0x11, 0x22, 0x33, 0x44, 0x00 };
        packet.systemExclusive7(5, UniversalMidiPacket.STATUS_SYSEX_COMPLETE,
                payload, 2, 4);
        assertEquals(0x35041122, packet.getWord(0));
        assertEquals(0x33440000, packet.getWord(1));
    }

    @Test
    public void testSysEx8() {
        UniversalMidiPacket packet = new UniversalMidiPacket();
        byte[] payload = new byte[]{ 0x00, 0x00, 0x11,
                0x22, 0x33, 0x44, 0x55,
                0x66, 0x77, (byte)0x88, (byte)0x99,
                (byte)0xAA, (byte)0xBB, 0x00 };
        packet.systemExclusive8(9, UniversalMidiPacket.STATUS_SYSEX_COMPLETE, 0x45,
                payload, 2, 11);
        assertEquals(0x590B4511, packet.getWord(0));
        assertEquals(0x22334455, packet.getWord(1));
        assertEquals(0x66778899, packet.getWord(2));
        assertEquals(0xAABB0000, packet.getWord(3));
    }

    @Test
    public void testMultiSysEx8() {
        UniversalMidiPacket packet = new UniversalMidiPacket();
        byte[] payload = new byte[2*13 + 11];
        for (int i = 0; i < payload.length; i++) {
            payload[i] = (byte)(0x10 + i);
        }

        packet.systemExclusive8(5, UniversalMidiPacket.STATUS_SYSEX_COMPLETE,
                0x34,
                payload, 2, payload.length - 3);
        assertEquals(0x35041122, packet.getWord(0));
        assertEquals(0x33440000, packet.getWord(1));
    }

    @NonNull
    private UniversalMidiPacket createOneWordPacket() {
        UniversalMidiPacket packet = UniversalMidiPacket.create();
        packet.setWord(UniversalMidiPacket.TYPE_UTILITY, 0x00FF0000); // has one word
        return packet;
    }

    @NonNull
    private UniversalMidiPacket createTwoWordPacket() {
        UniversalMidiPacket packet = UniversalMidiPacket.create();
        packet.setWord(0, 0x0000FF00);
        packet.setType(UniversalMidiPacket.TYPE_CHANNEL_VOICE_M2); // has two words
        packet.setWord(1, 0x000000FF);
        return packet;
    }

    @NonNull
    private UniversalMidiPacket createFourWordPacket() {
        UniversalMidiPacket packet = UniversalMidiPacket.create();
        packet.setWord(0, 0x00A5B6C7);
        packet.setType(0xF); // has 4 words
        packet.setWord(1, 0x00000080);
        packet.setWord(2, 0xFEDCBA98);
        packet.setWord(3, 0x01234567);
        return packet;
    }

    @Test
    public void testWrapperOneWord() {
        UniversalMidiPacket packet = createOneWordPacket();
        testWrappingPackets(packet);
    }


    @Test
    public void testWrapperTwoWords() {
        UniversalMidiPacket packet = createTwoWordPacket();
        testWrappingPackets(packet);
    }


    @Test
    public void testWrapperFourWords() {
        UniversalMidiPacket packet = createFourWordPacket();
        testWrappingPackets(packet);
    }


    @Test
    public void testWrapperMultiPackets() {
        UniversalMidiPacket[] packets = new UniversalMidiPacket[]{
                createFourWordPacket(),
                createOneWordPacket(),
                createTwoWordPacket()
        };
        testWrappingPackets(packets);
    }

    @Test
    public void testWrapperCC() {
        UniversalMidiPacket packet = UniversalMidiPacket.create();
        int index = 62;
        long value = 0x012345678;
        packet.controlChange(index, value);
        testWrappingPackets(packet);
    }

    // Test all of the encoding options.
    private void testWrappingPackets(UniversalMidiPacket packet) {
        UniversalMidiPacket[] packets = new UniversalMidiPacket[]{packet};
        testWrappingPackets(packets);
    }

    // Test all of the encoding options.
    private void testWrappingPackets(UniversalMidiPacket[] packets) {
        testWrappingPackets(packets,
                new SysExEncoder(),
                new SysExDecoder());
        testWrappingPackets(packets,
                new RawByteEncoder(),
                new RawByteDecoder());
    }

    private void testWrappingPackets(UniversalMidiPacket[] packets,
                                     PacketEncoder encoder, PacketDecoder decoder) {
        UniversalMidiPacket other = null;
        int len = 0;
        for (UniversalMidiPacket packet : packets) {
            len += encoder.encode(packet);
        }
        assertTrue(len > 0);
        decoder.wrap(encoder.getBytes(), 0, len);
        for (UniversalMidiPacket packet : packets) {
            other = UniversalMidiPacket.create();
            assertFalse(packet.equals(other));
            boolean done = decoder.decode(other);
            System.out.println("packet = " + packet);
            System.out.println("packet = " + packet);
            System.out.println("done  = " + done);
            assertTrue(done);
            assertTrue(packet.equals(other));
        }
        assertFalse(decoder.decode(other) );
    }

    static class TestSysExParser extends SysExParser {
        InquiryMessage message;

        @Override
        public void onInquiryMessage(InquiryMessage message) {
            this.message = message;
        }
    }

    @Test
    public void testMidiWriter() {
        MidiWriter buffer = new MidiWriter();
        buffer.write(0x1234A6);
        int i = 0;
        assertEquals(0xA6, buffer.getData()[i++] & 0xFF);
        buffer.write2(0x1352C7);
        assertEquals(0xC7,buffer.getData()[i++] & 0xFF);
        assertEquals(0x52,buffer.getData()[i++] & 0xFF);
        assertEquals(i, buffer.getCursor());

        buffer.write3(0x12345678);
        assertEquals(0x78,buffer.getData()[i++] & 0xFF);
        assertEquals(0x56,buffer.getData()[i++] & 0xFF);
        assertEquals(0x34,buffer.getData()[i++] & 0xFF);

        buffer.write4(0x15161718);
        assertEquals(0x18,buffer.getData()[i++] & 0xFF);
        assertEquals(0x17,buffer.getData()[i++] & 0xFF);
        assertEquals(0x16,buffer.getData()[i++] & 0xFF);
        assertEquals(0x15,buffer.getData()[i++] & 0xFF);
        assertEquals(i, buffer.getCursor());
    }

    @Test
    public void testMidiReader() {
        byte[] data = { 0x00, 0x55, 0x33, 0x44,
                0x77, 0x66, 0x19,
                0x01, 0x02, 0x03, 0x04 };
        MidiReader reader = new MidiReader(data, 1);

        assertEquals(0x00000055, reader.read());
        assertEquals(0x00004433, reader.read2());
        assertEquals(0x00196677, reader.read3());
        assertEquals(0x04030201, reader.read4());
    }

    @Test
    public void testInquiryMessage() {
        InquiryMessage message1 = new InquiryMessage(InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION);
        assertFalse(message1.supportsMidi20());
        message1.addProtocol(new ProtocolTypeMidi20());
        assertTrue(message1.supportsMidi20());
        message1.setMuid((102 << 21) + (23 << 14) + (97 << 7) + 45);

        MidiWriter buffer = new MidiWriter();
        int len = message1.encode(buffer);
        assertTrue(len > 0);

        byte[] expected = { (byte)0xF0, 0x7E, 0x7F, 0x0D,
                0x10, // CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION
                0x01, // CI_VERSION
                45, 97, 23, 102, // source MUID
                127, 127, 127, 127 // destination MUID
        };  // MUID
        byte[] actual = buffer.getData();
        int i = 0;
        for (byte b : expected) {
            System.out.printf("i = %d, expected = 0x%02X, actual = 0x%02X\n",
                    i, b, actual[i]);
            assertEquals(b, actual[i++]);
        }
    }

    @Test
    public void testParsing() throws IOException {
        InquiryMessage message1 = new InquiryMessage(
                InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION);
        message1.addProtocol(new ProtocolTypeMidi20());
        assertTrue(message1.supportsMidi20());

        MidiWriter buffer = new MidiWriter();

        int len = message1.encode(buffer);
        assertTrue(len > 0);

        TestSysExParser parser = new TestSysExParser();
        parser.parse(buffer.getData(), 0, buffer.getCursor());
        InquiryMessage message2 = parser.message;
        assertTrue(message2 != null);
        assertEquals(InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION, message2.getOpcode());
        assertTrue(message2.supportsMidi20());
    }

    // Pass messages between Negotiators to simulate a MIDI connection.
    private void negotiate(CapabilityNegotiator negotiator1, CapabilityNegotiator negotiator2) {
        System.out.println("negotiate 1<->2");
        int msec = 0;
        InquiryMessage message = null;
        boolean done1;
        boolean done2;
        do {
            System.out.println("1->2 @ nsec = " + msec);
            negotiator1.setTime(msec);
            negotiator2.setTime(msec);
            message = negotiator1.advanceStateMachine(message);
            if (message != null) {
                System.out.println("1->2 @ " + msec + ", " + message);
            }
            message = negotiator2.advanceStateMachine(message);
            if (message != null) {
                System.out.println("2->1 @ " + msec + ", " + message);
            }
            assertTrue(msec < 1000);
            msec += 7;
            done1 = negotiator1.isIdle() || negotiator1.isFinished();
            done2 = negotiator2.isIdle() || negotiator2.isFinished();
        } while (!(done1 && done2));
    }

    @Test
    public void testNegotiation2to2() throws IOException {
        CapabilityNegotiator negotiator1 = new CapabilityNegotiator();
        negotiator1.setSupportedVersion(Midi.VERSION_2_0);
        negotiator1.setInitiator(true); // INITIATE
        CapabilityNegotiator negotiator2 = new CapabilityNegotiator();
        negotiator2.setSupportedVersion(Midi.VERSION_2_0);

        negotiate(negotiator1, negotiator2);
        assertEquals(Midi.VERSION_2_0, negotiator1.getNegotiatedVersion());
        assertEquals(Midi.VERSION_2_0, negotiator2.getNegotiatedVersion());
    }

    @Test
    public void testNegotiation2to2ReverseInitiator() throws IOException {
        CapabilityNegotiator negotiator1 = new CapabilityNegotiator();
        negotiator1.setSupportedVersion(Midi.VERSION_2_0);
        CapabilityNegotiator negotiator2 = new CapabilityNegotiator();
        negotiator2.setSupportedVersion(Midi.VERSION_2_0);
        negotiator2.setInitiator(true); // INITIATE

        negotiate(negotiator1, negotiator2);
        assertEquals(Midi.VERSION_2_0, negotiator1.getNegotiatedVersion());
        assertEquals(Midi.VERSION_2_0, negotiator2.getNegotiatedVersion());
    }

    @Test
    public void testNegotiation2to2DualInitiator() throws IOException {
        CapabilityNegotiator negotiator1 = new CapabilityNegotiator();
        negotiator1.setSupportedVersion(Midi.VERSION_2_0);
        negotiator1.setInitiator(true); // INITIATE
        CapabilityNegotiator negotiator2 = new CapabilityNegotiator();
        negotiator2.setSupportedVersion(Midi.VERSION_2_0);
        negotiator2.setInitiator(true); // INITIATE

        negotiate(negotiator1, negotiator2);
        assertEquals(Midi.VERSION_2_0, negotiator1.getNegotiatedVersion());
        assertEquals(Midi.VERSION_2_0, negotiator2.getNegotiatedVersion());
    }

    @Test
    public void testNegotiation1to2() throws IOException {
        CapabilityNegotiator negotiator1 = new CapabilityNegotiator();
        negotiator1.setSupportedVersion(Midi.VERSION_1_0);
        CapabilityNegotiator negotiator2 = new CapabilityNegotiator();
        negotiator2.setSupportedVersion(Midi.VERSION_2_0);
        negotiator2.setInitiator(true);
        negotiate(negotiator1, negotiator2);
        assertEquals(Midi.VERSION_1_0, negotiator1.getNegotiatedVersion());
        assertEquals(Midi.VERSION_1_0, negotiator2.getNegotiatedVersion());
    }

    @Test
    public void testNegotiation2to1() throws IOException {
        CapabilityNegotiator negotiator1 = new CapabilityNegotiator();
        negotiator1.setSupportedVersion(Midi.VERSION_2_0);
        negotiator1.setInitiator(true);
        CapabilityNegotiator negotiator2 = new CapabilityNegotiator();
        negotiator2.setSupportedVersion(Midi.VERSION_1_0);
        negotiate(negotiator1, negotiator2);
        assertEquals(Midi.VERSION_1_0, negotiator1.getNegotiatedVersion());
        assertEquals(Midi.VERSION_1_0, negotiator2.getNegotiatedVersion());
    }

    @Test
    public void testNegotiation1to1() throws IOException {
        CapabilityNegotiator negotiator1 = new CapabilityNegotiator();
        negotiator1.setSupportedVersion(Midi.VERSION_1_0);
        CapabilityNegotiator negotiator2 = new CapabilityNegotiator();
        negotiator2.setSupportedVersion(Midi.VERSION_1_0);
        negotiate(negotiator1, negotiator2);
        assertEquals(Midi.VERSION_1_0, negotiator1.getNegotiatedVersion());
        assertEquals(Midi.VERSION_1_0, negotiator2.getNegotiatedVersion());
    }
}

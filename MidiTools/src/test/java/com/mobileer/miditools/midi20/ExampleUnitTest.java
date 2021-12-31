package com.mobileer.miditools.midi20;

import com.mobileer.miditools.midi20.inquiry.CapabilityNegotiator;
import com.mobileer.miditools.midi20.inquiry.InquiryMessage;
import com.mobileer.miditools.midi20.inquiry.ProtocolTypeMidiNew;
import com.mobileer.miditools.midi20.protocol.MidiPacketBase;
import com.mobileer.miditools.midi20.protocol.PacketDecoder;
import com.mobileer.miditools.midi20.protocol.PacketEncoder;
import com.mobileer.miditools.midi20.protocol.RawByteDecoder;
import com.mobileer.miditools.midi20.protocol.RawByteEncoder;
import com.mobileer.miditools.midi20.protocol.SysExDecoder;
import com.mobileer.miditools.midi20.protocol.SysExEncoder;
import com.mobileer.miditools.midi20.tools.Midi;
import com.mobileer.miditools.midi20.tools.MidiWriter;
import com.mobileer.miditools.midi20.tools.SysExParser;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Local unit test for Packet.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {

    // Test type, opcode, group, channel
    @Test
    public void packetBasics() {
        MidiPacketBase packet = MidiPacketBase.create();
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
        MidiPacketBase packet = MidiPacketBase.create();
        int noteNumber = 62;
        int velocity = 29876;
        packet.noteOn(noteNumber, velocity);
        packet.setChannel(0);
        assertTrue(packet.isNoteOn());
        assertFalse(packet.isNoteOff());
        assertEquals(noteNumber, packet.getNoteNumber());
        assertEquals(velocity, packet.getVelocity());

        packet.noteOff(noteNumber, velocity);
        assertFalse(packet.isNoteOn());
        assertTrue(packet.isNoteOff());
    }

    @Test
    public void testBasicControlChange() {
        MidiPacketBase packet = MidiPacketBase.create();
        int index = 62;
        int channel = 7;
        long value = 0x012345678;

        packet.controlChange(index, value);
        packet.setChannel(channel);
        assertTrue(packet.isControlChange());
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

        assertTrue(packet.isControlChange());
        assertEquals(index, packet.getControllerIndex());
        assertEquals(dvalue, packet.getNormalizedControllerValue(), 0.001);
    }

    @Test
    public void testProgramChange() {
        MidiPacketBase packet = new MidiPacketBase();
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
        MidiPacketBase packet = new MidiPacketBase();
        int index = (55 << 7) + 93;
        int channel = 14;
        long value = 0x098765432L;

        packet.RPN(index, value);
        packet.setChannel(channel);
        assertTrue(packet.isRPN());
        assertEquals(index, packet.getControllerIndex());
        assertEquals(value, packet.getControllerValue());
        assertEquals(channel, packet.getChannel());
    }

    @Test
    public void testNRPN() {
        MidiPacketBase packet = new MidiPacketBase();
        int index = (95 << 7) + 27;
        int channel = 5;
        long value = 0x056473829L;

        packet.NRPN(index, value);
        packet.setChannel(channel);
        assertTrue(packet.isNRPN());
        assertEquals(index, packet.getControllerIndex());
        assertEquals(value, packet.getControllerValue());
        assertEquals(channel, packet.getChannel());
    }

    @Test
    public void testWrapperOneWord() {
        MidiPacketBase packet = MidiPacketBase.create();
        packet.setWord(MidiPacketBase.TYPE_UTILITY, 0x00FF0000); // has one word
        testWrappingPacket(packet);
    }

    @Test
    public void testWrapperTwoWords() {
        MidiPacketBase packet = MidiPacketBase.create();
        packet.setWord(0, 0x0000FF00);
        packet.setType(MidiPacketBase.TYPE_CHANNEL_VOICE_HD); // has two words
        packet.setWord(1, 0x000000FF);
        testWrappingPacket(packet);
    }

    @Test
    public void testWrapperFourWords() {
        MidiPacketBase packet = MidiPacketBase.create();
        packet.setWord(0, 0x00A5B6C7);
        packet.setType(0xF); // has 4 words
        packet.setWord(1, 0x00000080);
        packet.setWord(2, 0xFEDCBA98);
        packet.setWord(3, 0x01234567);
        testWrappingPacket(packet);
    }

    @Test
    public void testWrapperCC() {
        MidiPacketBase packet = MidiPacketBase.create();
        int index = 62;
        long value = 0x012345678;
        packet.controlChange(index, value);
        testWrappingPacket(packet);
    }

    // Test all of the encoding options.
    private void testWrappingPacket(MidiPacketBase packet) {
        testWrappingPacket(packet,
                new SysExEncoder(),
                new SysExDecoder());
        testWrappingPacket(packet,
                new RawByteEncoder(),
                new RawByteDecoder());
    }

    private void testWrappingPacket(MidiPacketBase packet,
                                    PacketEncoder encoder, PacketDecoder decoder) {
        int len = encoder.encode(packet);

        MidiPacketBase other = MidiPacketBase.create();
        assertFalse(packet.equals(other));

        decoder.wrap(encoder.getBytes(), 0, len);
        boolean done = decoder.decode(other);
        System.out.println("packet = " + packet);
        System.out.println("other  = " + other);
        assertTrue(done);
        assertTrue(packet.equals(other));
    }

    static class TestSysExParser extends SysExParser {
        InquiryMessage message;

        @Override
        public void onInquiryMessage(InquiryMessage message) {
            this.message = message;
        }
    }

    @Test
    public void testMidiBuffer() {
        MidiWriter buffer = new MidiWriter();
        buffer.write(0x1234A6);
        int i = 0;
        assertEquals(0xA6, buffer.getData()[i++] & 0xFF);
        buffer.write2(0x1352C7);
        assertEquals(0x52,buffer.getData()[i++] & 0xFF);
        assertEquals(0xC7,buffer.getData()[i++] & 0xFF);
        assertEquals(i, buffer.getCursor());

        buffer.writeManufacturerId(0x09);
        assertEquals(0x09,buffer.getData()[i++] & 0xFF);
        assertEquals(i, buffer.getCursor());
    }

    @Test
    public void testInquiryMessage() {
        InquiryMessage message1 = new InquiryMessage(InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION);
        assertFalse(message1.supportsMidi20());
        message1.addProtocol(new ProtocolTypeMidiNew());
        assertTrue(message1.supportsMidi20());
        message1.setNegotiationIdentifier((102 << 21) + (23 << 14) + (97 << 7) + 45);

        MidiWriter buffer = new MidiWriter();
        int len = message1.encode(buffer);
        assertTrue(len > 0);

        byte[] expected = { (byte)0xF0, 0x7E, 0x7F, 0x0D, 0x10, 0x00,
                102, 23, 97, 45};
        byte[] actual = buffer.getData();
        int i = 0;
        for (byte b : expected) {
            assertEquals(b, actual[i++]);
        }
    }

    @Test
    public void testParsing() throws IOException {
        InquiryMessage message1 = new InquiryMessage(
                InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION);
        message1.addProtocol(new ProtocolTypeMidiNew());
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

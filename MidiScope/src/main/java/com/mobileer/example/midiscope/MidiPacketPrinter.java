package com.mobileer.example.midiscope;

import com.mobileer.miditools.midi20.protocol.UniversalMidiPacket;

/**
 * Format a MIDI 2.0 packet for printing.
 */
public class MidiPacketPrinter {

    static public String formatPacket(UniversalMidiPacket packet) {
        StringBuilder sb = new StringBuilder();
        int type = packet.getType();
        sb.append("\n          M2.0 t=" + type);
        sb.append(", ");
        switch(type) {
            case UniversalMidiPacket.TYPE_UTILITY:
                sb.append("UTILITY: ");
                break;
            case UniversalMidiPacket.TYPE_SYSTEM:
                sb.append("UTIL: ");
                break;
            case UniversalMidiPacket.TYPE_CHANNEL_VOICE_M1:
                sb.append("CV1: ");
                break;
            case UniversalMidiPacket.TYPE_DATA_64:
                sb.append("DATA64: ");
                break;
            case UniversalMidiPacket.TYPE_CHANNEL_VOICE_M2:
                sb.append("CV2: ");
                formatChannelVoiceHD(sb, packet);
                break;
            case UniversalMidiPacket.TYPE_DATA_128:
                sb.append("DATA128: ");
                break;
            default:
                sb.append("Unrecognized type");
                break;
        }
        return sb.toString();
    }


    private static void formatChannelVoiceHD(StringBuilder sb, UniversalMidiPacket packet) {
        int opcode = packet.getOpcode();
        sb.append(", op=" + opcode + ", ");
        switch(opcode) {
            case UniversalMidiPacket.OPCODE_NOTE_ON:
                sb.append("NOTE_ON");
                sb.append(", note#=" + packet.getNoteNumber());
                sb.append(", vel=" + packet.getVelocity());
                break;
            case UniversalMidiPacket.OPCODE_NOTE_OFF:
                sb.append("NOTE_OFF");
                sb.append(", note#=" + packet.getNoteNumber());
                sb.append(", vel=" + packet.getVelocity());
                break;
            case UniversalMidiPacket.OPCODE_PROGRAM_CHANGE:
                sb.append("PROGRAM_CHANGE");
                sb.append(", p#=" + packet.getProgram());
                sb.append(", bank=" + packet.getBank());
                break;
            default:
                sb.append("To Do");
                break;
        }
    }
}

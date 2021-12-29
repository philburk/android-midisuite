package com.mobileer.example.midiscope;

import com.mobileer.miditools.midi20.protocol.HDMidiPacket;
import com.mobileer.miditools.midi20.protocol.MidiPacketBase;

public class MidiPacketPrinter {

    static public String formatPacket(MidiPacketBase packet) {
        StringBuilder sb = new StringBuilder();
        int type = packet.getType();
        sb.append("\n          M2.0 t=" + type);
        sb.append(", ");
        switch(type) {
            case MidiPacketBase.TYPE_UTILITY:
                sb.append("UTILITY: ");
                break;
            case MidiPacketBase.TYPE_SYSTEM:
                sb.append("UTIL: ");
                break;
            case MidiPacketBase.TYPE_CHANNEL_VOICE:
                sb.append("CV: ");
                break;
            case MidiPacketBase.TYPE_DATA_64:
                sb.append("DATA64: ");
                break;
            case MidiPacketBase.TYPE_CHANNEL_VOICE_HD:
                sb.append("CVHD: ");
                formatChannelVoiceHD(sb, packet);
                break;
            case MidiPacketBase.TYPE_DATA_128:
                sb.append("DATA128: ");
                break;
            default:
                sb.append("Unrecognized type");
                break;
        }
        return sb.toString();
    }


    private static void formatChannelVoiceHD(StringBuilder sb, MidiPacketBase packet) {
        int opcode = packet.getOpcode();
        sb.append(", op=" + opcode + ", ");
        switch(opcode) {
            case HDMidiPacket.OPCODE_NOTE_ON:
                sb.append("NOTE_ON");
                sb.append(", note#=" + packet.getNoteNumber());
                sb.append(", vel=" + packet.getVelocity());
                break;
            case HDMidiPacket.OPCODE_NOTE_OFF:
                sb.append("NOTE_OFF");
                sb.append(", note#=" + packet.getNoteNumber());
                sb.append(", vel=" + packet.getVelocity());
                break;
            case HDMidiPacket.OPCODE_PROGRAM_CHANGE:
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

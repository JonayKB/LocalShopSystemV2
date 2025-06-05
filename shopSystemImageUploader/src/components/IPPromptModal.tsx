import React, { useState } from 'react';
import { Modal, View, Text, TextInput, StyleSheet, TouchableOpacity } from 'react-native';

type IPPromptProps = {
    visible: boolean;
    onSubmit: (text: string) => void;
};

const IPPromptModal = ({ visible, onSubmit }: IPPromptProps) => {
    const [text, setText] = useState('');

    return (
        <Modal visible={visible} transparent animationType="fade">
            <View style={styles.overlay}>
                <View style={styles.modalContainer}>
                    <Text style={styles.title}>Añadir IP del PC</Text>
                    <Text style={styles.description}>
                        Por favor, introduce la IP del ordenador con el servidor:
                    </Text>
                    <TextInput
                        style={styles.input}
                        value={text}
                        onChangeText={setText}
                        placeholder="Ej. 192.168.1.100"
                        keyboardType="numeric"
                        placeholderTextColor="#aaa"
                    />
                    <View style={styles.buttons}>
                        
                        <TouchableOpacity onPress={() => onSubmit(text)} style={styles.submitButton}>
                            <Text style={styles.submitText}>Aceptar</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </View>
        </Modal>
    );
};

export default IPPromptModal;

const styles = StyleSheet.create({
    overlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    modalContainer: {
        backgroundColor: 'white',
        padding: 20,
        borderRadius: 10,
        width: '80%',
    },
    title: {
        fontSize: 18,
        fontWeight: 'bold',
        marginBottom: 10,
        color: '#002f6c',
    },
    description: {
        fontSize: 14,
        marginBottom: 10,
        color: '#333',
    },
    input: {
        borderWidth: 1,
        borderColor: '#ccc',
        borderRadius: 5,
        paddingHorizontal: 10,
        height: 40,
        marginBottom: 20,
        color: '#000',
    },
    buttons: {
        flexDirection: 'row',
        justifyContent: 'flex-end',
    },
    cancelButton: {
        marginRight: 10,
    },
    cancelText: {
        color: '#999',
    },
    submitButton: {
        backgroundColor: '#002f6c',
        paddingHorizontal: 15,
        paddingVertical: 8,
        borderRadius: 5,
    },
    submitText: {
        color: 'white',
    },
});

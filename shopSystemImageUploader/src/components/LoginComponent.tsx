import { Alert, Button, StyleSheet, Text, TextInput, View } from 'react-native'
import React, { useEffect, useState } from 'react'
import AuthRepository from '../repositories/AuthRepository';
import { AxiosError } from 'axios';
import IPPromptModal from './IPPromptModal';

type Props = {
    setToken: (token: string | null) => void;
    ip: string | undefined;
}

const LoginComponent = (props: Props) => {
    const authRepository = new AuthRepository();
    const [email, setEmail] = useState<string | null>(null)
    const [password, setPassword] = useState<string | null>(null);





    const handleLogin = async () => {
        if (!props.ip) {
            return;
        }
        if (!email || !password) {
            return;
        }
        try {
            const response = await authRepository.login(email, password, props.ip);
            if (response) {
                props.setToken(response);

            }
        } catch (error) {
            console.error("Login error:", error);
            if (error instanceof AxiosError && error.response) {
                if (error.response.status === 401) {
                    Alert.alert("Login Failed", "Invalid email or password. Please try again.");
                } else {
                    Alert.alert("Login Failed", "An unexpected error occurred. Please try again later.");
                }
            } else {
                Alert.alert("Login Failed", "An unexpected error occurred. Please try again later.");
            }

            return;

        }

    }

    return (
        <View style={styles.container}>

            <Text style={styles.title}>Login</Text>
            <Text style={styles.label}>Email:</Text>
            <TextInput
                style={styles.input}
                value={email ?? ''}
                onChangeText={setEmail}
                placeholder="Enter your email"
                keyboardType="email-address"
                placeholderTextColor="#a0a0a0"
            />
            <Text style={styles.label}>Password:</Text>
            <TextInput
                style={styles.input}
                value={password ?? ''}
                onChangeText={setPassword}
                placeholder="Enter your password"
                secureTextEntry
                placeholderTextColor="#a0a0a0"
            />
            <View style={styles.buttonContainer}>
                <Button title="Login" onPress={handleLogin} color="#002f6c" />
            </View>
        </View>
    )
}

export default LoginComponent

const styles = StyleSheet.create({
    container: {
        flex: 1,
        padding: 20,
        justifyContent: 'center',
        backgroundColor: '#f2f6fa',
    },
    title: {
        fontSize: 24,
        fontWeight: 'bold',
        color: '#002f6c',
        marginBottom: 20,
        textAlign: 'center',
    },
    label: {
        fontSize: 16,
        color: '#002f6c',
        marginBottom: 5,
    },
    input: {
        height: 40,
        borderColor: '#002f6c',
        borderWidth: 1,
        borderRadius: 5,
        marginBottom: 15,
        paddingHorizontal: 10,
        backgroundColor: 'white',
        color: '#002f6c',
    },
    buttonContainer: {
        marginTop: 10,
    }
});

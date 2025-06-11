import React from 'react';
import { View, Button, Alert, Platform } from 'react-native';
import ImagePicker from 'react-native-image-crop-picker';
import axios from 'axios';

type Props = {
    itemId: number;
    token: string | null;
    ip: string | undefined;
};

export default function ImageUploader(props: Readonly<Props>): React.JSX.Element {
    const handlePick = async (fromCamera: boolean) => {


        ImagePicker.openCamera({
            mediaType: "photo",
            cropping: true,
            width: 800,
            height: 800,
        }).then((image) => {
            console.log(image);
        });



    };

    const uploadImage = async (uri: string) => {
        const filename = uri.split('/').pop() ?? 'photo.jpg';
        const filetype = filename.endsWith('.png') ? 'image/png' : 'image/jpeg';

        const formData = new FormData();
        formData.append('file', {
            uri: Platform.OS === 'android' ? uri : uri.replace('file://', ''),
            type: filetype,
            name: filename,
        });

        try {
            const response = await axios.post(`http://${props.ip}/upload`, formData, {
                headers: {
                    'Content-Type': 'multipart/form-data',
                    Authorization: `Bearer ${props.token}`,
                },
            });

            Alert.alert('Éxito', 'Imagen subida correctamente');
        } catch (error) {
            console.error(error);
            Alert.alert('Error', 'No se pudo subir la imagen');
        }
    };

    return (
        <View style={{ padding: 20 }}>
            <Button title="Tomar foto con cámara" onPress={() => handlePick(true)} />
            <View style={{ height: 10 }} />
            <Button title="Elegir de la galería" onPress={() => handlePick(false)} />
        </View>
    );
}

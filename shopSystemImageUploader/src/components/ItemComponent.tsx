import React, { useEffect, useState } from 'react';
import { View, Text, Image, StyleSheet, TouchableOpacity } from 'react-native';
import Item from '../models/Item';
import ImageRespository from '../repositories/ImageRepository';

type Props = {
    item: Item;
    token: string | null;
    onClick?: (item: Item) => void;
    ip: string | undefined;
};

const ItemComponent = (props: Props) => {
    const imageRepository = new ImageRespository();
    const [imageUrl, setImageUrl] = useState<string>();

    useEffect(() => {
        const fetchImage = async () => {
            try {
                const url = await imageRepository.getImageById(props.item.image, props.token, props.ip);
                setImageUrl(url);
            } catch (error) {
                setImageUrl(
                    'https://static.vecteezy.com/system/resources/previews/008/695/917/non_2x/no-image-available-icon-simple-two-colors-template-for-no-image-or-picture-coming-soon-and-placeholder-illustration-isolated-on-white-background-vector.jpg'
                );
            }
        };

        fetchImage();
    }, [props.item.image, imageRepository, props.token]);

    const Container = props.onClick ? TouchableOpacity : View;

    return (
        <Container
            style={styles.container}
            onPress={props.onClick ? () => props.onClick!(props.item) : undefined}
            activeOpacity={0.7}
        >
            <Image
                source={{ uri: imageUrl }}
                style={styles.image}
                resizeMode="cover"
            />
            <Text style={styles.title}>{props.item.name}</Text>
            <Text style={styles.price}>{props.item.price.toFixed(2)} €</Text>
        </Container>
    );
};

const styles = StyleSheet.create({
    container: {
        alignItems: 'center',
        backgroundColor: '#3a3d4a',
        padding: 10,
        borderRadius: 8,
        elevation: 2,
        height: '100%',
        marginVertical: 8,
        marginHorizontal: 2,
        width: '32%',
    },
    image: {
        width: '100%',
        height: 120,
        borderRadius: 8,
        marginBottom: 8,
        backgroundColor: '#222',
    },
    title: {
        textTransform: 'capitalize',
        color: 'white',
        fontSize: 18,
        overflow: 'hidden',
        fontWeight: 'bold',
        marginVertical: 4,
    },
    price: {
        color: 'white',
        fontSize: 16,
    },
});

export default ItemComponent;
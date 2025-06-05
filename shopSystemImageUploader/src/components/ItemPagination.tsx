import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, ActivityIndicator, TouchableOpacity, StyleSheet } from 'react-native';
import Item from '../models/Item';
import Page from '../models/Page';
import axios from 'axios';
import BaseInfoRepository from '../utils/BaseInfoRepository';
import ItemComponent from './ItemComponent';

type Props = {
    categoryId?: number;
    token: string | null,
    onItemClick?: (item: Item) => void;
    text?: string;
    sortBy?: string;
    ascending?: boolean;
    ip: string | undefined;
}

const pageSize = 12;

const ItemPagination = (props: Props) => {
    const { categoryId, token, ip } = props;
    const [pageData, setPageData] = useState<Page<Item> | null>(null);
    const [page, setPage] = useState(0);
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        const fetchItems = async () => {
            setLoading(true);
            let url = `items/${page}/${pageSize}`;

            const res = await axios.get(BaseInfoRepository.getBaseUrl(ip) + url, {
                params: {
                    sortBy: props.sortBy ?? 'name',
                    ascending: props.ascending ?? true,
                    name: props.text ?? '',
                },
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });
            setPageData(res.data);
            setLoading(false);
        };
        fetchItems();
    }, [page, categoryId, token, props.text, props.sortBy, props.ascending]);

    const renderContent = () => {
        if (loading) {
            return <ActivityIndicator color="#fff" size="large" style={{ marginTop: 32 }} />;
        } else if (!pageData || pageData.content.length === 0) {
            return <Text style={styles.emptyText}>No hay productos.</Text>;
        } else {
            return (
                <FlatList
                    data={pageData.content}
                    keyExtractor={item => item.id.toString()}
                    numColumns={3}
                    contentContainerStyle={styles.grid}
                    renderItem={({ item }) => (
                        <ItemComponent item={item} token={token} onClick={props.onItemClick} ip={ip} />
                    )}
                />
            );
        }
    };

    return (
        <View style={styles.container}>
            <View style={styles.gridContainer}>
                {renderContent()}
            </View>
            <View style={styles.pagination}>
                <TouchableOpacity
                    onPress={() => setPage(p => Math.max(0, p - 1))}
                    disabled={!pageData || pageData.first}
                    style={[
                        styles.button,
                        (!pageData || pageData.first) && styles.buttonDisabled
                    ]}
                >
                    <Text style={styles.buttonText}>Anterior</Text>
                </TouchableOpacity>
                <Text style={styles.pageInfo}>
                    Página {pageData ? pageData.number + 1 : 0} de {pageData ? pageData.totalPages : 0}
                </Text>
                <TouchableOpacity
                    onPress={() => setPage(p => pageData ? Math.min(pageData.totalPages - 1, p + 1) : p)}
                    disabled={!pageData || pageData.last || pageData.totalPages === 0}
                    style={[
                        styles.button,
                        (!pageData || pageData.last || pageData.totalPages === 0) && styles.buttonDisabled
                    ]}
                >
                    <Text style={styles.buttonText}>Siguiente</Text>
                </TouchableOpacity>
            </View>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#1e1e2f',
        borderRadius: 8,
        padding: 12,
    },
    gridContainer: {
        flex: 1,
        minHeight: 300,
        maxHeight: '100%',
        marginBottom: 16,
    },
    grid: {
        gap: 20,
        padding: 10,
    },
    emptyText: {
        color: 'white',
        fontSize: 18,
        textAlign: 'center',
        marginTop: 32,
    },
    pagination: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginTop: 24,
    },
    button: {
        backgroundColor: '#1e1e2f',
        borderRadius: 4,
        paddingVertical: 8,
        paddingHorizontal: 16,
        marginHorizontal: 8,
        opacity: 1,
    },
    buttonDisabled: {
        opacity: 0.5,
    },
    buttonText: {
        color: 'white',
        fontSize: 16,
    },
    pageInfo: {
        color: 'white',
        fontSize: 16,
        marginHorizontal: 12,
    },
});

export default ItemPagination;
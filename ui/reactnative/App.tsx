import React, { useEffect, useState } from 'react';
import {
    SafeAreaView,
    StatusBar,
    StyleSheet,
    Text,
    useColorScheme,
    View,
    FlatList,
    ActivityIndicator,
} from 'react-native';
import {
    Colors,
    Header,
} from 'react-native/Libraries/NewAppScreen';

type WorkflowEvent = {
    eventId: string;
    detailType: string;
    status: string;
};

const API_HOST = 'solance-cluster-alb-1606409103.eu-west-1.elb.amazonaws.com';

export default function App(): React.JSX.Element {
    const isDark = useColorScheme() === 'dark';
    const bgColor = isDark ? Colors.darker : Colors.lighter;

    const [events, setEvents] = useState<WorkflowEvent[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetch(`http://${API_HOST}/api/v1/events`)
            .then(r => r.json())
            .then((data: WorkflowEvent[]) => setEvents(data))
            .catch(console.error)
            .finally(() => setLoading(false));
    }, []);

    // header displayed above list items
    const ListHeader = () => (
        <>
            <Header />
            <View style={[styles.container, { backgroundColor: bgColor }]}>
                <Text style={styles.heading}>Workflow Events</Text>
            </View>
        </>
    );

    return (
        <SafeAreaView style={[styles.safeArea, { backgroundColor: bgColor }]}>
            <StatusBar
                barStyle={isDark ? 'light-content' : 'dark-content'}
                backgroundColor={bgColor}
            />

            {loading ? (
                <ActivityIndicator style={styles.loader} size="large" />
            ) : (
                <FlatList
                    data={events}
                    keyExtractor={item => item.eventId}
                    ListHeaderComponent={ListHeader}
                    contentContainerStyle={styles.contentContainer}
                    ListEmptyComponent={
                        <Text style={styles.empty}>No events received</Text>
                    }
                    renderItem={({ item }) => (
                        <View style={styles.item}>
                            <Text style={styles.eventType}>{item.detailType}</Text>
                            <Text style={styles.eventId}>ID: {item.eventId}</Text>
                            <Text>Status: {item.status}</Text>
                        </View>
                    )}
                />
            )}
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    safeArea: { flex: 1 },
    container: {
        paddingHorizontal: 16,
        paddingVertical: 24,
    },
    heading: {
        fontSize: 28,
        fontWeight: '700',
    },
    loader: {
        flex: 1,
        justifyContent: 'center',
    },
    contentContainer: {
        paddingHorizontal: 16,
        paddingBottom: 24,
    },
    item: {
        backgroundColor: '#fff',
        padding: 12,
        marginVertical: 6,
        borderRadius: 8,
        // shadows
        shadowColor: '#000',
        shadowOpacity: 0.1,
        shadowRadius: 4,
        shadowOffset: { width: 0, height: 2 },
        elevation: 2,
    },
    eventType: {
        fontSize: 16,
        fontWeight: '600',
    },
    eventId: {
        fontSize: 12,
        color: '#666',
        marginBottom: 4,
    },
    empty: {
        textAlign: 'center',
        marginTop: 32,
        color: '#888',
    },
});

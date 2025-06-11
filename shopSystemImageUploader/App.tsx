import React, { useState } from 'react';
import { View } from 'react-native';
import LoginComponent from './src/components/LoginComponent';
import ItemPagination from './src/components/ItemPagination';
import IPPromptModal from './src/components/IPPromptModal';
import ImageUploader from './src/components/ImageUploader';


function App(): React.JSX.Element {
  const [token, setToken] = useState<string | null>(null);
  const [pcIP, setPcIP] = useState<string | undefined>(undefined);
  const [itemId, setItemId] = useState<number | undefined>(undefined);

  console.log('App component rendered with token:', token);

  return (
    <View style={{ flex: 1, backgroundColor: '#f0f0f0' }}>

      {token ? (
        <>
          <ItemPagination
            token={token}
            onItemClick={(item) => setItemId(item.id)}
            sortBy="name"
            ascending={true}
            ip={pcIP}

          />
          {itemId && (
            <ImageUploader
              itemId={itemId}
              token={token}
              ip={pcIP}
            />
          )}
        </>
      ) : (
        <>
          <IPPromptModal visible={!pcIP} onSubmit={(text) => setPcIP(text)} />
          <LoginComponent setToken={setToken} ip={pcIP} />
        </>
      )}
    </View>

  );
}



export default App;

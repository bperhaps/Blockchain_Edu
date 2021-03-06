package WebServer.handler;

import blockchainCore.BlockchainCore;
import blockchainCore.blockchain.Block;
import blockchainCore.blockchain.transaction.Transaction;
import blockchainCore.blockchain.transaction.TxInput;
import blockchainCore.blockchain.transaction.TxOutput;
import blockchainCore.blockchain.wallet.Wallet;
import blockchainCore.node.network.Node;
import blockchainCore.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WebSocketHandler {
    public BlockchainCore bcCore = new BlockchainCore();
    Gson gson = new Gson();

    public String createNode() {
        return bcCore.createNode();
    }
    public void destoryNode(Object data) {
        String nodeId = (String)data;

        bcCore.destoryNode(nodeId);
    }
    public Wallet createWallet(Object data) {
        String nodeId = (String)data;

        String address = bcCore.createWallet(nodeId);
        return bcCore.getNode(nodeId).getWallet(address);
    }
    public void createConnection(Object data) {
        Map<String, Object> dataMap = (LinkedTreeMap<String, Object>)data;
        String source = (String)dataMap.get("src");
        String destination = (String)dataMap.get("dest");

        bcCore.createConnection(source, destination);
    }
    public void destroyConnection(Object data) {
        Map<String, Object> dataMap = (LinkedTreeMap<String, Object>)data;
        String source = (String)dataMap.get("src");
        String destination = (String)dataMap.get("dest");

        bcCore.destroyConnection(source, destination);
    }
    public void endTransmission(Object data) {
        Map<String, Object> dataMap = (LinkedTreeMap<String, Object>)data;
        String nodeId = (String)dataMap.get("nodeId");
        String BlockHash = (String)dataMap.get("block");

        bcCore.endTransmission(nodeId, BlockHash);
    }
    public void sendBTC(Object data) {
        Map<String, Object> dataMap = (LinkedTreeMap<String, Object>)data;
        String nodeId = (String)dataMap.get("nodeId");
        String from = (String)dataMap.get("from");
        String to = (String)dataMap.get("to");
        int amount = ((Double)dataMap.get("amount")).intValue();

        bcCore.sendBTC(nodeId, from, to, amount);
    }

    public String bcTipFromNodeId(String nodeId) {
        return Utils.toHexString(bcCore.getNode(nodeId).getBlockChain().getTip());
    }
    public HashMap<String, Object> nodeInf(String nodeId) {
        Node node = bcCore.getNode(nodeId);
        HashMap<String, Object> nodeInf = new HashMap<>();

        nodeInf.put("id", node.getNodeId());
        nodeInf.put("knownNodes", gson.toJson(node.getNetwork().getConnList()));
        nodeInf.put("transactionPool", transactionInfs(node.getTxsFromTxPool()));
        nodeInf.put("blocks", blockInfs(node.getBlockChain().getBlocks()));
        nodeInf.put("wallets", walletInfs(node.getWallets()));

        return nodeInf;
    }

    public ArrayList<HashMap<String, Object>> walletInfs(ArrayList<Wallet> wallets) {
        ArrayList<HashMap<String, Object>> walletInfs = new ArrayList<>();

        for ( Wallet wallet : wallets ){
            walletInfs.add(walletInf(wallet));
        }

        return walletInfs;

    }
    public HashMap<String, Object> walletInf(Wallet wallet) {
        HashMap<String, Object> walletInf = new HashMap<>();
        walletInf.put("address", wallet.getAddress());
        walletInf.put("privateKey", Utils.toHexString(wallet.getPrivateKey().getEncoded()));
        walletInf.put("publickKey", Utils.toHexString(wallet.getPublicKey().getEncoded()));

        return walletInf;
    }
    public HashMap<String, Integer> walletsBlanace(String nodeId) {
        return bcCore.getNode(nodeId).getBalances();
    }

    public ArrayList<HashMap<String, Object>> blockInfs(ArrayList<Block> blocks) {
        ArrayList<HashMap<String, Object>> blockInfs = new ArrayList<>();

        for (Block block : blocks) {
            blockInfs.add(blockInf(block));
        }

        return blockInfs;
    }


    public HashMap<String, Object> blockInf(Block block) {
        HashMap<String, Object> blockInf = new HashMap<>();
        blockInf.put("timestamp", block.getTimestamp());
        blockInf.put("transactions", transactionInfs(new ArrayList<Transaction>(Arrays.asList(block.getTransactions()))));
        blockInf.put("prevBlockHash", Utils.toHexString(block.getPrevBlockHash()));
        blockInf.put("hash", Utils.toHexString(block.getHash()));
        blockInf.put("nonce", block.getNonce());
        blockInf.put("height", block.getHeight());

        return blockInf;
    }


    public ArrayList<HashMap<String, Object>> transactionInfs(ArrayList<Transaction> txs) {
        ArrayList<HashMap<String, Object>> txInfs = new ArrayList<>();

        for(Transaction tx : txs) {
            txInfs.add(transactionInf(tx));
        }

        return txInfs;
    }

    public HashMap<String, Object> transactionInf(Transaction tx) {
        HashMap<String, Object> txInf = new HashMap<>();
        txInf.put("id", Utils.toHexString(tx.getId()));
        txInf.put("transactionInput", txInputInfs(tx.getVin()));
        txInf.put("transactionOutput", txOutputInfs(tx.getVout()));

        return txInf;
    }

    public ArrayList<HashMap<String, Object>> txOutputInfs(ArrayList<TxOutput> txOutputs){
        ArrayList<HashMap<String, Object>> txOnputInfs = new ArrayList<>();

        for(TxOutput txOutput : txOutputs) {
            txOnputInfs.add(txOutputInf(txOutput));
        }

        return txOnputInfs;
    }

    public HashMap<String, Object> txOutputInf(TxOutput txOutput) {
        HashMap<String, Object> txInputInf = new HashMap<>();
        txInputInf.put("value", txOutput.getValue());
        txInputInf.put("pubKeyHash", Utils.toHexString(txOutput.getPublicKeyHash()));

        return txInputInf;
    }

    public ArrayList<HashMap<String, Object>> txInputInfs(ArrayList<TxInput> txinputs){
        ArrayList<HashMap<String, Object>> txInputInfs = new ArrayList<>();

        for(TxInput txinput : txinputs) {
            txInputInfs.add(txInputInf(txinput));
        }

        return txInputInfs;
    }

    public HashMap<String, Object> txInputInf(TxInput txinput) {
        HashMap<String, Object> txInputInf = new HashMap<>();
        txInputInf.put("txId", Utils.toHexString(txinput.getTxId()));
        txInputInf.put("outputIndex", txinput.getvOut());
        if(txinput.getPubKey() == null) txInputInf.put("pubkey", "");
        else txInputInf.put("pubkey", Utils.toHexString(txinput.getPubKey().getEncoded()));
        txInputInf.put("signature", Utils.toHexString(txinput.getSignature()));

        return txInputInf;
    }
}

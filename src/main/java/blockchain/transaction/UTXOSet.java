package blockchain.transaction;

import DB.Bucket;
import DB.Cursor;
import DB.Db;
import blockchain.Block;
import blockchain.Blockchain;
import utils.Pair;
import utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class UTXOSet {
    private final String utxoBucket = "chainstate";
    private Blockchain bc;
    private Db db;

    public UTXOSet(Blockchain bc) {
        this.bc = bc;
        this.db = bc.getDb();
    }
    public void reIndex() {
        db.getBucket(utxoBucket).clear();
        HashMap<String, TxOutputs> UTXO = bc.findUTXO();

        Iterator<String> itr = UTXO.keySet().iterator();
        while(itr.hasNext()){
            String txId = itr.next();
            TxOutputs outs = UTXO.get(txId);

            db.getBucket(utxoBucket).put(txId, Utils.toBytes(outs));
        }

    }

    public ArrayList<TxOutput> findUTXO(byte[] pubkeyHash) {
        ArrayList<TxOutput> UTXOs = new ArrayList();

        Bucket b = db.getBucket(utxoBucket);
        Cursor c = b.Cursor();

        while(c.hasNext()){
            Pair<String, byte[]> kv = c.next();
            TxOutputs outs = (TxOutputs)Utils.toObject(kv.getValue());
            for(TxOutput out : outs.getOutputs()) {
                if(out.isLockedWithKey(pubkeyHash)) {
                    UTXOs.add(out);
                }
            }
        }

        return UTXOs;
    }

    public void update(Block block) {
        Bucket b = db.getBucket(utxoBucket);

        for(Transaction tx : block.getTransactions()) {
            if(!tx.isCoinBase()) {
                for(TxInput vin : tx.getVin()) {
                    TxOutputs updatedOuts = new TxOutputs();
                    TxOutputs outs = (TxOutputs)Utils.toObject(b.get(Utils.byteArrayToHexString(vin.getTxId())));

                    for(int outIdx = 0; outIdx < outs.getOutputs().size(); outIdx++) {
                        TxOutput out = outs.getOutputs().get(outIdx);
                        if(outIdx !=  vin.getvOut()) {
                            updatedOuts.getOutputs().add(out);
                        }
                    }

                    if(updatedOuts.getOutputs().size() == 0 ) b.delete(Utils.byteArrayToHexString(vin.getTxId()));
                    else b.put(Utils.byteArrayToHexString(vin.getTxId()), Utils.toBytes(updatedOuts));
                }
            }
            TxOutputs newOutputs = new TxOutputs();
            for(TxOutput out : tx.getVout()) newOutputs.getOutputs().add(out);

            b.put(Utils.byteArrayToHexString(tx.getId()), Utils.toBytes(newOutputs));
        }

    }

    public Pair<Integer, HashMap<String, ArrayList<Integer>>> findSpendableOutputs(byte[] pubkeyHash, int amount) {
        HashMap<String, ArrayList<Integer>> unspentOutputs = new HashMap();
        int accumulated = 0;

        Bucket b = db.getBucket(utxoBucket);
        Cursor c = b.Cursor();

        while(c.hasNext()) {
            Pair<String, byte[]> kv = c.next();
            String txId = kv.getKey();
            TxOutputs outs = (TxOutputs)Utils.toObject(kv.getValue());

            for(int outIdx=0; outIdx < outs.getOutputs().size(); outIdx++) {
                TxOutput out = outs.getOutputs().get(outIdx);
                if(out.isLockedWithKey(pubkeyHash) && accumulated < amount) {
                    accumulated+= out.getValue();
                    if(unspentOutputs.get(txId) == null) unspentOutputs.put(txId, new ArrayList<>());
                    unspentOutputs.get(txId).add(outIdx);
                }
            }
        }

        return new Pair(accumulated, unspentOutputs);
    }

    public Blockchain getBc() {
        return bc;
    }

}
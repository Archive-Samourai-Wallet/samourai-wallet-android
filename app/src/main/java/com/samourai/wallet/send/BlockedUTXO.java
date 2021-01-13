package com.samourai.wallet.send;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.samourai.wallet.util.LogUtil.debug;

public class BlockedUTXO {

    private static BlockedUTXO instance = null;
    private static ConcurrentHashMap<String,Long> blockedUTXO = null;
    private static CopyOnWriteArrayList<String> notDustedUTXO = null;
    private static ConcurrentHashMap<String,Long> blockedUTXOPostMix = null;
    private static ConcurrentHashMap<String,Long> blockedUTXOBadBank = null;
    private static CopyOnWriteArrayList<String> notDustedUTXOPostMix = null;

    public final static long BLOCKED_UTXO_THRESHOLD = 1001L;

    private BlockedUTXO() { ; }

    public static BlockedUTXO getInstance() {

        if(instance == null) {

            debug("BlockedUTXO", "create instance");

            instance = new BlockedUTXO();
            blockedUTXO = new ConcurrentHashMap<>();
            notDustedUTXO = new CopyOnWriteArrayList<>();
            blockedUTXOPostMix = new ConcurrentHashMap<>();
            blockedUTXOBadBank = new ConcurrentHashMap<>();
            notDustedUTXOPostMix = new CopyOnWriteArrayList<>();
        }

        return instance;
    }

    public long get(String hash, int idx)    {
        return blockedUTXO.get(hash + "-" + Integer.toString(idx));
    }

    public void add(String hash, int idx, long value)    {
        blockedUTXO.put(hash + "-" + Integer.toString(idx), value);
        debug("BlockedUTXO", "add:" + hash + "-" + Integer.toString(idx));
    }

    public void remove(String hash, int idx)   {
        if(blockedUTXO != null && blockedUTXO.containsKey(hash + "-" + Integer.toString(idx)))  {
            blockedUTXO.remove(hash + "-" + Integer.toString(idx));
            debug("BlockedUTXO", "remove:" + hash + "-" + Integer.toString(idx));
        }
    }

    public void remove(String id)   {
        if(blockedUTXO != null && blockedUTXO.containsKey(id))  {
            blockedUTXO.remove(id);
            debug("BlockedUTXO", "remove:" + id);
        }
    }

    public boolean contains(String hash, int idx)   {
        return blockedUTXO.containsKey(hash + "-" + Integer.toString(idx));
    }

    public void clear()    {
        blockedUTXO.clear();
        debug("BlockedUTXO", "clear");
    }

    public long getTotalValueBlocked0()  {
        long ret = 0L;
        for(String id : blockedUTXO.keySet())   {
            ret += blockedUTXO.get(id);
        }
        return ret;
    }

    public long getTotalValuePostMix()  {
        long ret = 0L;
        for(String id : blockedUTXOPostMix.keySet())   {
            ret += blockedUTXOPostMix.get(id);
        }
        return ret;
    }

    public long getTotalValueBadBank()  {
        long ret = 0L;
        for(String id : blockedUTXOBadBank.keySet())   {
            ret += blockedUTXOBadBank.get(id);
        }
        return ret;
    }

    public void addNotDusted(String hash, int idx)    {
        if(!notDustedUTXO.contains(hash + "-" + Integer.toString(idx)))    {
            notDustedUTXO.add(hash + "-" + Integer.toString(idx));
        }
    }

    public void addNotDusted(String id)    {
        if(!notDustedUTXO.contains(id))    {
            notDustedUTXO.add(id);
        }
    }

    public void addNotDustedPostMix(String hash, int idx)    {
        if(!notDustedUTXOPostMix.contains(hash + "-" + Integer.toString(idx)))    {
            notDustedUTXOPostMix.add(hash + "-" + Integer.toString(idx));
        }
    }

    public void addNotDustedPostMix(String id)    {
        if(!notDustedUTXOPostMix.contains(id))    {
            notDustedUTXOPostMix.add(id);
        }
    }

    public void removeNotDusted(String hash, int idx)   {
        if(notDustedUTXO.contains(hash + "-" + Integer.toString(idx)))    {
            notDustedUTXO.remove(hash + "-" + Integer.toString(idx));
        }
    }

    public void removeNotDusted(String s)   {
        if(notDustedUTXO.contains(s))    {
            notDustedUTXO.remove(s);
        }
    }

    public void removeNotDustedPostMix(String hash, int idx)   {
        if(notDustedUTXOPostMix.contains(hash + "-" + Integer.toString(idx)))    {
            notDustedUTXOPostMix.remove(hash + "-" + Integer.toString(idx));
        }
    }

    public void removeNotDustedPostMix(String s)   {
        if(notDustedUTXOPostMix.contains(s))    {
            notDustedUTXOPostMix.remove(s);
        }
    }

    public boolean containsNotDusted(String hash, int idx)   {
        return notDustedUTXO.contains(hash + "-" + Integer.toString(idx));
    }

    public boolean containsNotDustedPostMix(String hash, int idx)   {
        return notDustedUTXOPostMix.contains(hash + "-" + Integer.toString(idx));
    }

    public ConcurrentHashMap<String, Long> getBlockedUTXO() {
        return blockedUTXO;
    }

    public ConcurrentHashMap<String, Long> getBlockedUTXOBadBank() {
        return blockedUTXOBadBank;
    }

    public long getBadBank(String hash, int idx)    {
        return blockedUTXOBadBank.get(hash + "-" + Integer.toString(idx));
    }

    public void addBadBank(String hash, int idx, long value)    {
        blockedUTXOBadBank.put(hash + "-" + Integer.toString(idx), value);
        debug("BlockedUTXO", "add:" + hash + "-" + Integer.toString(idx));
    }

    public void removeBadBank(String hash, int idx)   {
        if(blockedUTXOBadBank != null && blockedUTXOBadBank.containsKey(hash + "-" + Integer.toString(idx)))  {
            blockedUTXOBadBank.remove(hash + "-" + Integer.toString(idx));
            debug("BlockedUTXO", "remove:" + hash + "-" + Integer.toString(idx));
        }
    }

    public void removeBadBank(String id)   {
        if(blockedUTXOBadBank != null && blockedUTXOBadBank.containsKey(id))  {
            blockedUTXOBadBank.remove(id);
            debug("BlockedUTXO", "remove:" + id);
        }
    }

    public boolean containsBadBank(String hash, int idx)   {
        return blockedUTXOBadBank.containsKey(hash + "-" + Integer.toString(idx));
    }

    public void clearBadBank()    {
        blockedUTXOBadBank.clear();
        debug("BlockedUTXO", "clear");
    }

    public long getTotalValueBlockedBadBank()  {
        long ret = 0L;
        for(String id : blockedUTXOBadBank.keySet())   {
            debug("BlockedUTXO", "bad bank blocked:" + id);
            ret += blockedUTXOBadBank.get(id);
        }
        debug("BlockedUTXO", "bad bank blocked:" + ret);
        return ret;
    }

    public ConcurrentHashMap<String, Long> getBlockedUTXOPostMix() {
        return blockedUTXOPostMix;
    }

    public List<String> getNotDustedUTXO() {
        return notDustedUTXO;
    }

    public List<String> getNotDustedUTXOPostMix() {
        return notDustedUTXOPostMix;
    }

    public long getPostMix(String hash, int idx)    {
        return blockedUTXOPostMix.get(hash + "-" + Integer.toString(idx));
    }

    public void addPostMix(String hash, int idx, long value)    {
        blockedUTXOPostMix.put(hash + "-" + Integer.toString(idx), value);
        debug("BlockedUTXO", "add:" + hash + "-" + Integer.toString(idx));
    }

    public void removePostMix(String hash, int idx)   {
        if(blockedUTXOPostMix != null && blockedUTXOPostMix.containsKey(hash + "-" + Integer.toString(idx)))  {
            blockedUTXOPostMix.remove(hash + "-" + Integer.toString(idx));
            debug("BlockedUTXO", "remove:" + hash + "-" + Integer.toString(idx));
        }
    }

    public void removePostMix(String id)   {
        if(blockedUTXOPostMix != null && blockedUTXOPostMix.containsKey(id))  {
            blockedUTXOPostMix.remove(id);
            debug("BlockedUTXO", "remove:" + id);
        }
    }

    public boolean containsPostMix(String hash, int idx)   {
        return blockedUTXOPostMix.containsKey(hash + "-" + Integer.toString(idx));
    }

    public void clearPostMix()    {
        blockedUTXOPostMix.clear();
        debug("BlockedUTXO", "clear");
    }

    public long getTotalValueBlockedPostMix()  {
        long ret = 0L;
        for(String id : blockedUTXOPostMix.keySet())   {
            debug("BlockedUTXO", "post-mix blocked:" + id);
            ret += blockedUTXOPostMix.get(id);
        }
        debug("BlockedUTXO", "post-mix blocked:" + ret);
        return ret;
    }

    public JSONObject toJSON() {

        JSONObject blockedObj = new JSONObject();

        JSONArray array = new JSONArray();
        JSONArray arrayPostMix = new JSONArray();
        JSONArray arrayBadBank = new JSONArray();
        try {
            for(String id : blockedUTXO.keySet())   {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("value", blockedUTXO.get(id));
                array.put(obj);
            }
            blockedObj.put("blocked", array);

            JSONArray notDusted = new JSONArray();
            for(String s : notDustedUTXO)   {
                notDusted.put(s);
            }
            blockedObj.put("notDusted", notDusted);

            JSONArray notDustedPostMix = new JSONArray();
            for(String s : notDustedUTXOPostMix)   {
                notDustedPostMix.put(s);
            }
            blockedObj.put("notDustedPostMix", notDustedPostMix);

            for(String id : blockedUTXOPostMix.keySet())   {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("value", blockedUTXOPostMix.get(id));
                arrayPostMix.put(obj);
            }
            blockedObj.put("blockedPostMix", arrayPostMix);

            for(String id : blockedUTXOBadBank.keySet())   {
                JSONObject obj = new JSONObject();
                obj.put("id", id);
                obj.put("value", blockedUTXOBadBank.get(id));
                arrayBadBank.put(obj);
            }
            blockedObj.put("blockedBadBank", arrayBadBank);

        }
        catch(JSONException je) {
            ;
        }

        return blockedObj;
    }

    public void fromJSON(JSONObject blockedObj) {

        blockedUTXO.clear();
        blockedUTXOPostMix.clear();
        blockedUTXOBadBank.clear();
        notDustedUTXO.clear();

        try {

            if(blockedObj.has("blocked"))    {
                JSONArray array = blockedObj.getJSONArray("blocked");

                for(int i = 0; i < array.length(); i++)   {
                    JSONObject obj = array.getJSONObject(i);
                    blockedUTXO.put(obj.getString("id"), obj.getLong("value"));
                }
            }

            if(blockedObj.has("notDusted"))  {
                JSONArray array = blockedObj.getJSONArray("notDusted");

                for(int i = 0; i < array.length(); i++)   {
                    addNotDusted(array.getString(i));
                }
            }

            if(blockedObj.has("notDustedPostMix"))  {
                JSONArray array = blockedObj.getJSONArray("notDustedPostMix");

                for(int i = 0; i < array.length(); i++)   {
                    addNotDustedPostMix(array.getString(i));
                }
            }

            if(blockedObj.has("blockedPostMix"))    {
                JSONArray array = blockedObj.getJSONArray("blockedPostMix");

                for(int i = 0; i < array.length(); i++)   {
                    JSONObject obj = array.getJSONObject(i);
                    blockedUTXOPostMix.put(obj.getString("id"), obj.getLong("value"));
                }
            }

            if(blockedObj.has("blockedBadBank"))    {
                JSONArray array = blockedObj.getJSONArray("blockedBadBank");

                for(int i = 0; i < array.length(); i++)   {
                    JSONObject obj = array.getJSONObject(i);
                    blockedUTXOBadBank.put(obj.getString("id"), obj.getLong("value"));
                }
            }

        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }

    }

}

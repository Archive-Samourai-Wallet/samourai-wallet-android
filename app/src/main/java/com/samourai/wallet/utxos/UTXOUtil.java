package com.samourai.wallet.utxos;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


public class UTXOUtil {

    private static UTXOUtil instance = null;

    private static HashMap<String, List<String>> utxoAutoTags = null;
    private static HashMap<String,String> utxoNotes = null;
    private static HashMap<String,Integer> utxoScores = null;

    private UTXOUtil() {
        ;
    }

    public static UTXOUtil getInstance() {

        if(instance == null) {
            utxoAutoTags = new HashMap<String,List<String>>();
            utxoNotes = new HashMap<String,String>();
            utxoScores = new HashMap<String,Integer>();
            instance = new UTXOUtil();
        }

        return instance;
    }

    public void reset() {
        utxoAutoTags.clear();
        utxoNotes.clear();
        utxoScores.clear();
    }

    public void add(String hash, int idx, String tag) {
        add(hash + "-" + idx, tag);
    }

    public void add(String utxo, String tag) {
        if(utxoAutoTags.containsKey(utxo) && !utxoAutoTags.get(utxo).contains(tag)) {
            utxoAutoTags.get(utxo).add(tag);
        }
        else {
            List<String> tags = new ArrayList<String>();
            tags.add(tag);
            utxoAutoTags.put(utxo, tags);
        }
    }

    public List<String> get(String hash, int idx) {
        if (utxoAutoTags.containsKey(hash + "-" + idx)) {
            return utxoAutoTags.get(hash + "-" + idx);
        } else {
            return null;
        }

    }

    public List<String> get(String utxo) {
        if (utxoAutoTags.containsKey(utxo)) {
            return utxoAutoTags.get(utxo);
        } else {
            return null;
        }

    }

    public HashMap<String, List<String>> getTags() {
        return utxoAutoTags;
    }

    public void remove(String hash, int idx) {
        utxoAutoTags.remove(hash + "-" + idx);
    }

    public void remove(String utxo) {
        utxoAutoTags.remove(utxo);
    }

    public void addNote(String hash, String note) {
        if (isNotBlank(note)) {
            utxoNotes.put(hash, note);
        }
    }

    public String getNote(String hash) {
        return utxoNotes.containsKey(hash)
                ? defaultIfBlank(utxoNotes.get(hash), null)
                : null;
    }

    public HashMap<String,String> getNotes() {
        return utxoNotes;
    }

    public void removeNote(String hash) {
        utxoNotes.remove(hash);
    }

    public void addScore(String utxo, int score) {
        utxoScores.put(utxo, score);
    }

    public int getScore(String utxo) {
        if(utxoScores.containsKey(utxo))  {
            return utxoScores.get(utxo);
        }
        else    {
            return 0;
        }

    }

    public void incScore(String utxo, int score) {
        if(utxoScores.containsKey(utxo))  {
            utxoScores.put(utxo, utxoScores.get(utxo) + score);
        }
        else    {
            utxoScores.put(utxo, score);
        }

    }

    public HashMap<String,Integer> getScores() {
        return utxoScores;
    }

    public void removeScore(String utxo) {
        utxoScores.remove(utxo);
    }

    public JSONArray toJSON() {

        JSONArray utxos = new JSONArray();
        for (String key : utxoAutoTags.keySet()) {
            List<String> tags = utxoAutoTags.get(key);
            List<String> _tags = new ArrayList<String>(new HashSet<String>(tags));
            for(String t : _tags) {
                JSONArray tag = new JSONArray();
                tag.put(key);
                tag.put(t);
                utxos.put(tag);
            }
        }

        return utxos;
    }

    public void fromJSON(JSONArray utxos) {

        utxoAutoTags.clear();

        try {
            for (int i = 0; i < utxos.length(); i++) {
                JSONArray tag = (JSONArray) utxos.get(i);

                if(utxoAutoTags.containsKey((String) tag.get(0)) && !utxoAutoTags.get((String) tag.get(0)).contains((String) tag.get(1))) {
                    utxoAutoTags.get((String) tag.get(0)).add((String) tag.get(1));
                }
                else     {
                    List<String> tags = new ArrayList<String>();
                    tags.add((String) tag.get(1));
                    utxoAutoTags.put((String) tag.get(0), tags);
                }

            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public JSONArray toJSON_notes() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoNotes.keySet()) {
            JSONArray note = new JSONArray();
            note.put(key);
            note.put(utxoNotes.get(key));
            utxos.put(note);
        }

        return utxos;
    }

    public void fromJSON_notes(JSONArray utxos) {

        utxoNotes.clear();

        try {
            for(int i = 0; i < utxos.length(); i++) {
                final JSONArray note = (JSONArray)utxos.get(i);
                final String txHash = (String) note.get(0);
                final Object rawNoteContent = note.get(1);
                // check type because null value is encapsulated into JSONObject
                utxoNotes.put(
                        txHash,
                        (rawNoteContent instanceof String) ? (String)rawNoteContent : null);
            }
        } catch(final JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    public JSONArray toJSON_scores() {

        JSONArray utxos = new JSONArray();
        for(String key : utxoScores.keySet()) {
            JSONArray score = new JSONArray();
            score.put(key);
            score.put(utxoScores.get(key));
            utxos.put(score);
        }

        return utxos;
    }

    public void fromJSON_scores(JSONArray utxos) {

        utxoScores.clear();

        try {
            for(int i = 0; i < utxos.length(); i++) {
                JSONArray score = (JSONArray)utxos.get(i);
                utxoScores.put((String)score.get(0), (int)score.get(1));
            }
        }
        catch(JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

}

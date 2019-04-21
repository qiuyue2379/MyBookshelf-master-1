package com.kunfei.bookshelf.help;

import android.content.SharedPreferences;

import com.kunfei.bookshelf.DbHelper;
import com.kunfei.bookshelf.MApplication;
import com.kunfei.bookshelf.bean.BookShelfBean;
import com.kunfei.bookshelf.bean.BookSourceBean;
import com.kunfei.bookshelf.bean.ReplaceRuleBean;
import com.kunfei.bookshelf.bean.SearchHistoryBean;
import com.kunfei.bookshelf.model.BookSourceManager;
import com.kunfei.bookshelf.model.ReplaceRuleManager;
import com.kunfei.bookshelf.utils.FileUtils;
import com.kunfei.bookshelf.utils.GsonUtils;
import com.kunfei.bookshelf.utils.XmlUtils;

import java.io.FileInputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by GKF on 2018/1/30.
 * 数据恢复
 */

public class DataRestore {

    public static DataRestore getInstance() {
        return new DataRestore();
    }

    public Boolean run() throws Exception {
        String dirPath = FileUtils.getSdCardPath() + "/YueDu";
        restoreConfig(dirPath);
        restoreBookSource(dirPath);
        restoreBookShelf(dirPath);
        restoreSearchHistory(dirPath);
        restoreReplaceRule(dirPath);
        return true;
    }

    private void restoreConfig(String dirPath) {
        Map<String, ?> entries = null;
        try (FileInputStream ins = new FileInputStream(dirPath + "/config.xml")) {
            entries = XmlUtils.readMapXml(ins);
        } catch (Exception ignored) {
        }
        if (entries == null || entries.isEmpty()) return;
        long donateHb = MApplication.getConfigPreferences().getLong("DonateHb", 0);
        donateHb = donateHb > System.currentTimeMillis() ? 0 : donateHb;
        SharedPreferences.Editor editor = MApplication.getConfigPreferences().edit();
        editor.clear();
        for (Map.Entry<String, ?> entry : entries.entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            String type = v.getClass().getSimpleName();

            switch (type) {
                case "Integer":
                    editor.putInt(key, (Integer) v);
                    break;
                case "Boolean":
                    editor.putBoolean(key, (Boolean) v);
                    break;
                case "String":
                    editor.putString(key, (String) v);
                    break;
                case "Float":
                    editor.putFloat(key, (Float) v);
                    break;
                case "Long":
                    editor.putLong(key, (Long) v);
                    break;
            }
        }
        editor.putLong("DonateHb", donateHb);
        editor.putInt("versionCode", MApplication.getVersionCode());
        editor.apply();
        MApplication.getInstance().upThemeStore();
    }

    private void restoreBookShelf(String file) throws Exception {
        String json = DocumentHelper.readString("myBookShelf.json", file);
        if (json != null) {
            List<BookShelfBean> bookShelfList = GsonUtils.parseJArray(json, BookShelfBean.class);
            for (BookShelfBean bookshelf : bookShelfList) {
                if (bookshelf.getNoteUrl() != null) {
                    DbHelper.getDaoSession().getBookShelfBeanDao().insertOrReplace(bookshelf);
                }
                if (bookshelf.getBookInfoBean().getNoteUrl() != null) {
                    DbHelper.getDaoSession().getBookInfoBeanDao().insertOrReplace(bookshelf.getBookInfoBean());
                }
            }
        }
    }

    private void restoreBookSource(String file) throws Exception {
        String json = DocumentHelper.readString("myBookSource.json", file);
        if (json != null) {
            List<BookSourceBean> bookSourceBeans = GsonUtils.parseJArray(json, BookSourceBean.class);
            BookSourceManager.addBookSource(bookSourceBeans);
        }
    }

    private void restoreSearchHistory(String file) throws Exception {
        String json = DocumentHelper.readString("myBookSearchHistory.json", file);
        if (json != null) {
            List<SearchHistoryBean> searchHistoryBeans = GsonUtils.parseJArray(json, SearchHistoryBean.class);
            if (searchHistoryBeans != null && searchHistoryBeans.size() > 0) {
                DbHelper.getDaoSession().getSearchHistoryBeanDao().insertOrReplaceInTx(searchHistoryBeans);
            }
        }
    }

    private void restoreReplaceRule(String file) throws Exception {
        String json = DocumentHelper.readString("myBookReplaceRule.json", file);
        if (json != null) {
            List<ReplaceRuleBean> replaceRuleBeans = GsonUtils.parseJArray(json, ReplaceRuleBean.class);
            ReplaceRuleManager.addDataS(replaceRuleBeans);
        }
    }
}
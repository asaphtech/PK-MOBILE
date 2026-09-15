package com.pkmobile.keyboard.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ShortcutDao_Impl implements ShortcutDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ShortcutEntity> __insertionAdapterOfShortcutEntity;

  private final EntityDeletionOrUpdateAdapter<ShortcutEntity> __deletionAdapterOfShortcutEntity;

  private final EntityDeletionOrUpdateAdapter<ShortcutEntity> __updateAdapterOfShortcutEntity;

  private final SharedSQLiteStatement __preparedStmtOfSetPackageActive;

  private final SharedSQLiteStatement __preparedStmtOfSetShortcutActive;

  private final SharedSQLiteStatement __preparedStmtOfSetShortcutActiveInPreset;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByPreset;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByPresetAndTrigger;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByPackage;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByTriggerCode;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ShortcutDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfShortcutEntity = new EntityInsertionAdapter<ShortcutEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `shortcuts` (`preset_id`,`trigger_code`,`expansion_text`,`category`,`expansion_mode`,`package_name`,`is_active`,`created_at`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShortcutEntity entity) {
        statement.bindString(1, entity.getPresetId());
        statement.bindString(2, entity.getTriggerCode());
        statement.bindString(3, entity.getExpansionText());
        if (entity.getCategory() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCategory());
        }
        statement.bindString(5, entity.getExpansionMode());
        statement.bindString(6, entity.getPackageName());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfShortcutEntity = new EntityDeletionOrUpdateAdapter<ShortcutEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `shortcuts` WHERE `preset_id` = ? AND `trigger_code` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShortcutEntity entity) {
        statement.bindString(1, entity.getPresetId());
        statement.bindString(2, entity.getTriggerCode());
      }
    };
    this.__updateAdapterOfShortcutEntity = new EntityDeletionOrUpdateAdapter<ShortcutEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `shortcuts` SET `preset_id` = ?,`trigger_code` = ?,`expansion_text` = ?,`category` = ?,`expansion_mode` = ?,`package_name` = ?,`is_active` = ?,`created_at` = ? WHERE `preset_id` = ? AND `trigger_code` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShortcutEntity entity) {
        statement.bindString(1, entity.getPresetId());
        statement.bindString(2, entity.getTriggerCode());
        statement.bindString(3, entity.getExpansionText());
        if (entity.getCategory() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getCategory());
        }
        statement.bindString(5, entity.getExpansionMode());
        statement.bindString(6, entity.getPackageName());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(7, _tmp);
        statement.bindLong(8, entity.getCreatedAt());
        statement.bindString(9, entity.getPresetId());
        statement.bindString(10, entity.getTriggerCode());
      }
    };
    this.__preparedStmtOfSetPackageActive = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE shortcuts SET is_active = ? WHERE package_name = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetShortcutActive = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE shortcuts SET is_active = ? WHERE trigger_code = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetShortcutActiveInPreset = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE shortcuts SET is_active = ? WHERE preset_id = ? AND trigger_code = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByPreset = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shortcuts WHERE preset_id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByPresetAndTrigger = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shortcuts WHERE preset_id = ? AND trigger_code = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByPackage = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shortcuts WHERE package_name = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteByTriggerCode = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shortcuts WHERE trigger_code = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shortcuts";
        return _query;
      }
    };
  }

  @Override
  public Object insertOrUpdate(final ShortcutEntity shortcut,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfShortcutEntity.insert(shortcut);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<ShortcutEntity> shortcuts,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfShortcutEntity.insert(shortcuts);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final ShortcutEntity shortcut,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfShortcutEntity.handle(shortcut);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final ShortcutEntity shortcut,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        int _total = 0;
        __db.beginTransaction();
        try {
          _total += __updateAdapterOfShortcutEntity.handle(shortcut);
          __db.setTransactionSuccessful();
          return _total;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object setPackageActive(final String packageName, final boolean isActive,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetPackageActive.acquire();
        int _argIndex = 1;
        final int _tmp = isActive ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, packageName);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetPackageActive.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setShortcutActive(final String triggerCode, final boolean isActive,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetShortcutActive.acquire();
        int _argIndex = 1;
        final int _tmp = isActive ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, triggerCode);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetShortcutActive.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setShortcutActiveInPreset(final String presetId, final String triggerCode,
      final boolean isActive, final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetShortcutActiveInPreset.acquire();
        int _argIndex = 1;
        final int _tmp = isActive ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, presetId);
        _argIndex = 3;
        _stmt.bindString(_argIndex, triggerCode);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfSetShortcutActiveInPreset.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByPreset(final String presetId,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByPreset.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, presetId);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByPreset.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByPresetAndTrigger(final String presetId, final String triggerCode,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByPresetAndTrigger.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, presetId);
        _argIndex = 2;
        _stmt.bindString(_argIndex, triggerCode);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByPresetAndTrigger.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByPackage(final String packageName,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByPackage.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, packageName);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByPackage.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByTriggerCode(final String triggerCode,
      final Continuation<? super Integer> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByTriggerCode.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, triggerCode);
        try {
          __db.beginTransaction();
          try {
            final Integer _result = _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return _result;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteByTriggerCode.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllShortcuts(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ShortcutEntity>> getAllFlow() {
    final String _sql = "SELECT * FROM shortcuts ORDER BY trigger_code ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shortcuts"}, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllList(final Continuation<? super List<ShortcutEntity>> $completion) {
    final String _sql = "SELECT * FROM shortcuts ORDER BY trigger_code ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ShortcutEntity>> getActiveFlow() {
    final String _sql = "\n"
            + "        SELECT s.* FROM shortcuts s \n"
            + "        LEFT JOIN presets p ON s.preset_id = p.id \n"
            + "        WHERE (p.is_active = 1 OR NOT EXISTS (SELECT 1 FROM presets WHERE is_active = 1)) \n"
            + "          AND s.is_active = 1 \n"
            + "        ORDER BY s.trigger_code ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shortcuts",
        "presets"}, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ShortcutEntity>> getShortcutsByActivePresetFlow() {
    final String _sql = "\n"
            + "        SELECT s.* FROM shortcuts s \n"
            + "        LEFT JOIN presets p ON s.preset_id = p.id \n"
            + "        WHERE (p.is_active = 1 OR NOT EXISTS (SELECT 1 FROM presets WHERE is_active = 1)) \n"
            + "        ORDER BY s.trigger_code ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shortcuts",
        "presets"}, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<ShortcutEntity>> getShortcutsByPresetFlow(final String presetId) {
    final String _sql = "SELECT * FROM shortcuts WHERE preset_id = ? ORDER BY trigger_code ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, presetId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shortcuts"}, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getShortcutsByPresetList(final String presetId,
      final Continuation<? super List<ShortcutEntity>> $completion) {
    final String _sql = "SELECT * FROM shortcuts WHERE preset_id = ? ORDER BY trigger_code ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, presetId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object findByShortcut(final String key,
      final Continuation<? super ShortcutEntity> $completion) {
    final String _sql = "SELECT * FROM shortcuts WHERE LOWER(trigger_code) = LOWER(?) LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShortcutEntity>() {
      @Override
      @Nullable
      public ShortcutEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final ShortcutEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object findActiveByShortcut(final String key,
      final Continuation<? super ShortcutEntity> $completion) {
    final String _sql = "\n"
            + "        SELECT s.* FROM shortcuts s \n"
            + "        LEFT JOIN presets p ON s.preset_id = p.id \n"
            + "        WHERE (p.is_active = 1 OR NOT EXISTS (SELECT 1 FROM presets WHERE is_active = 1)) \n"
            + "          AND s.is_active = 1 \n"
            + "          AND LOWER(s.trigger_code) = LOWER(?) \n"
            + "        LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ShortcutEntity>() {
      @Override
      @Nullable
      public ShortcutEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPresetId = CursorUtil.getColumnIndexOrThrow(_cursor, "preset_id");
          final int _cursorIndexOfTriggerCode = CursorUtil.getColumnIndexOrThrow(_cursor, "trigger_code");
          final int _cursorIndexOfExpansionText = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_text");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfExpansionMode = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion_mode");
          final int _cursorIndexOfPackageName = CursorUtil.getColumnIndexOrThrow(_cursor, "package_name");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "is_active");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final ShortcutEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPresetId;
            _tmpPresetId = _cursor.getString(_cursorIndexOfPresetId);
            final String _tmpTriggerCode;
            _tmpTriggerCode = _cursor.getString(_cursorIndexOfTriggerCode);
            final String _tmpExpansionText;
            _tmpExpansionText = _cursor.getString(_cursorIndexOfExpansionText);
            final String _tmpCategory;
            if (_cursor.isNull(_cursorIndexOfCategory)) {
              _tmpCategory = null;
            } else {
              _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            }
            final String _tmpExpansionMode;
            _tmpExpansionMode = _cursor.getString(_cursorIndexOfExpansionMode);
            final String _tmpPackageName;
            _tmpPackageName = _cursor.getString(_cursorIndexOfPackageName);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new ShortcutEntity(_tmpPresetId,_tmpTriggerCode,_tmpExpansionText,_tmpCategory,_tmpExpansionMode,_tmpPackageName,_tmpIsActive,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<String>> getDistinctPackagesFlow() {
    final String _sql = "SELECT DISTINCT package_name FROM shortcuts ORDER BY package_name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shortcuts"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object countByPreset(final String presetId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM shortcuts WHERE preset_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, presetId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

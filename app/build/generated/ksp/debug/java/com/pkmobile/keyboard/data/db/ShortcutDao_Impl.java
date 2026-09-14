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
import java.lang.Long;
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

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public ShortcutDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfShortcutEntity = new EntityInsertionAdapter<ShortcutEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `shortcuts` (`id`,`shortcut`,`expansion`,`created_at`) VALUES (nullif(?, 0),?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShortcutEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getShortcut());
        statement.bindString(3, entity.getExpansion());
        statement.bindLong(4, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfShortcutEntity = new EntityDeletionOrUpdateAdapter<ShortcutEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `shortcuts` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShortcutEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfShortcutEntity = new EntityDeletionOrUpdateAdapter<ShortcutEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR REPLACE `shortcuts` SET `id` = ?,`shortcut` = ?,`expansion` = ?,`created_at` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ShortcutEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getShortcut());
        statement.bindString(3, entity.getExpansion());
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM shortcuts WHERE id = ?";
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
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfShortcutEntity.insertAndReturnId(shortcut);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertAll(final List<ShortcutEntity> shortcuts,
      final Continuation<? super List<Long>> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<List<Long>>() {
      @Override
      @NonNull
      public List<Long> call() throws Exception {
        __db.beginTransaction();
        try {
          final List<Long> _result = __insertionAdapterOfShortcutEntity.insertAndReturnIdsList(shortcuts);
          __db.setTransactionSuccessful();
          return _result;
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
  public Object deleteById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteById.release(_stmt);
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
  public Flow<List<ShortcutEntity>> getAllFlow() {
    final String _sql = "SELECT * FROM shortcuts ORDER BY shortcut ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"shortcuts"}, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShortcut = CursorUtil.getColumnIndexOrThrow(_cursor, "shortcut");
          final int _cursorIndexOfExpansion = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpShortcut;
            _tmpShortcut = _cursor.getString(_cursorIndexOfShortcut);
            final String _tmpExpansion;
            _tmpExpansion = _cursor.getString(_cursorIndexOfExpansion);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpId,_tmpShortcut,_tmpExpansion,_tmpCreatedAt);
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
    final String _sql = "SELECT * FROM shortcuts ORDER BY shortcut ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ShortcutEntity>>() {
      @Override
      @NonNull
      public List<ShortcutEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShortcut = CursorUtil.getColumnIndexOrThrow(_cursor, "shortcut");
          final int _cursorIndexOfExpansion = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final List<ShortcutEntity> _result = new ArrayList<ShortcutEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ShortcutEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpShortcut;
            _tmpShortcut = _cursor.getString(_cursorIndexOfShortcut);
            final String _tmpExpansion;
            _tmpExpansion = _cursor.getString(_cursorIndexOfExpansion);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new ShortcutEntity(_tmpId,_tmpShortcut,_tmpExpansion,_tmpCreatedAt);
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
    final String _sql = "SELECT * FROM shortcuts WHERE LOWER(shortcut) = LOWER(?) LIMIT 1";
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
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfShortcut = CursorUtil.getColumnIndexOrThrow(_cursor, "shortcut");
          final int _cursorIndexOfExpansion = CursorUtil.getColumnIndexOrThrow(_cursor, "expansion");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "created_at");
          final ShortcutEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpShortcut;
            _tmpShortcut = _cursor.getString(_cursorIndexOfShortcut);
            final String _tmpExpansion;
            _tmpExpansion = _cursor.getString(_cursorIndexOfExpansion);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new ShortcutEntity(_tmpId,_tmpShortcut,_tmpExpansion,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}

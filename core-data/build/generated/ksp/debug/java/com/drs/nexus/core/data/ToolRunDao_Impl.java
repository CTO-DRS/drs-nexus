package com.drs.nexus.core.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
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
public final class ToolRunDao_Impl implements ToolRunDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ToolRunEntity> __insertionAdapterOfToolRunEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  private final SharedSQLiteStatement __preparedStmtOfClearAll;

  public ToolRunDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfToolRunEntity = new EntityInsertionAdapter<ToolRunEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `tool_runs` (`id`,`toolId`,`inputPreview`,`outputPreview`,`executedAt`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ToolRunEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getToolId());
        statement.bindString(3, entity.getInputPreview());
        statement.bindString(4, entity.getOutputPreview());
        statement.bindLong(5, entity.getExecutedAt());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tool_runs WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM tool_runs";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final ToolRunEntity run, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfToolRunEntity.insertAndReturnId(run);
          __db.setTransactionSuccessful();
          return _result;
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
  public Object clearAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAll.acquire();
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
          __preparedStmtOfClearAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ToolRunEntity>> recentRuns(final int limit) {
    final String _sql = "SELECT * FROM tool_runs ORDER BY executedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tool_runs"}, new Callable<List<ToolRunEntity>>() {
      @Override
      @NonNull
      public List<ToolRunEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfToolId = CursorUtil.getColumnIndexOrThrow(_cursor, "toolId");
          final int _cursorIndexOfInputPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "inputPreview");
          final int _cursorIndexOfOutputPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "outputPreview");
          final int _cursorIndexOfExecutedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "executedAt");
          final List<ToolRunEntity> _result = new ArrayList<ToolRunEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ToolRunEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpToolId;
            _tmpToolId = _cursor.getString(_cursorIndexOfToolId);
            final String _tmpInputPreview;
            _tmpInputPreview = _cursor.getString(_cursorIndexOfInputPreview);
            final String _tmpOutputPreview;
            _tmpOutputPreview = _cursor.getString(_cursorIndexOfOutputPreview);
            final long _tmpExecutedAt;
            _tmpExecutedAt = _cursor.getLong(_cursorIndexOfExecutedAt);
            _item = new ToolRunEntity(_tmpId,_tmpToolId,_tmpInputPreview,_tmpOutputPreview,_tmpExecutedAt);
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
  public Flow<List<ToolRunEntity>> runsForTool(final String toolId, final int limit) {
    final String _sql = "SELECT * FROM tool_runs WHERE toolId = ? ORDER BY executedAt DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, toolId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"tool_runs"}, new Callable<List<ToolRunEntity>>() {
      @Override
      @NonNull
      public List<ToolRunEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfToolId = CursorUtil.getColumnIndexOrThrow(_cursor, "toolId");
          final int _cursorIndexOfInputPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "inputPreview");
          final int _cursorIndexOfOutputPreview = CursorUtil.getColumnIndexOrThrow(_cursor, "outputPreview");
          final int _cursorIndexOfExecutedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "executedAt");
          final List<ToolRunEntity> _result = new ArrayList<ToolRunEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ToolRunEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpToolId;
            _tmpToolId = _cursor.getString(_cursorIndexOfToolId);
            final String _tmpInputPreview;
            _tmpInputPreview = _cursor.getString(_cursorIndexOfInputPreview);
            final String _tmpOutputPreview;
            _tmpOutputPreview = _cursor.getString(_cursorIndexOfOutputPreview);
            final long _tmpExecutedAt;
            _tmpExecutedAt = _cursor.getLong(_cursorIndexOfExecutedAt);
            _item = new ToolRunEntity(_tmpId,_tmpToolId,_tmpInputPreview,_tmpOutputPreview,_tmpExecutedAt);
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
  public Object count(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM tool_runs";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
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

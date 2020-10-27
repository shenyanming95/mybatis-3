/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.executor;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;

import org.apache.ibatis.cursor.Cursor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.transaction.Transaction;

/**
 * @author Clinton Begin
 */
public class SimpleExecutor extends BaseExecutor {

  public SimpleExecutor(Configuration configuration, Transaction transaction) {
    super(configuration, transaction);
  }

  @Override
  public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
      stmt = prepareStatement(handler, ms.getStatementLog());
      return handler.update(stmt);
    } finally {
      closeStatement(stmt);
    }
  }

  @Override
  public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
    // java.sql.Statement对象
    Statement stmt = null;
    try {
      Configuration configuration = ms.getConfiguration();
      // 通过mybatis全局配置对象Configuration, 创建StatementHandler.实际上由它来执行SQL
      // 每次查询都会new一个StatementHandler
      StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
      // 真正创建java.sql.Statement实例, 并为其赋值参数,
      stmt = prepareStatement(handler, ms.getStatementLog());
      // 到此一个完整的java.sql.Statement就已经创建好了, 可以调用StatementHandler.query()
      // 方法查询数据库了, 转到StatementHandler.query().
      return handler.query(stmt, resultHandler);
    } finally {
      // 查询完会关闭数据资源java.sql.Statement, 即执行：statement.close();
      // 通过JDBC知识, 关闭一个资源, 只会关掉以它做底层的上级资源. 所以statement关掉
      // 不会关闭数据库链接Connection, 但会关闭查询结果ResultSets
      closeStatement(stmt);
    }
  }

  @Override
  protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
    Configuration configuration = ms.getConfiguration();
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, null, boundSql);
    Statement stmt = prepareStatement(handler, ms.getStatementLog());
    Cursor<E> cursor = handler.queryCursor(stmt);
    stmt.closeOnCompletion();
    return cursor;
  }

  @Override
  public List<BatchResult> doFlushStatements(boolean isRollback) {
    return Collections.emptyList();
  }

  private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
    Statement stmt;
    // 调用抽象父类BaseExecutor的Transaction对象的getConnection()获取数据库链接
    Connection connection = getConnection(statementLog);
    // 通过StatementHandler.prepare()方法创建Statement实例, 并为它设置查询超时时间。
    // 前面分析知道, 此时的StatementHandler为PreparedStatementHandler实现类, 它就会通过
    // 数据库链接connection的prepareStatement()方法创建出PreparedStatement实例, 这个就是
    // 原生jdbc的代码。而PreparedStatement实例由数据库驱动(如mysql-driver)提供..
    stmt = handler.prepare(connection, transaction.getTimeout());
    // 为创建好的Statement设置参数值, 例如 PrepareStatement 对象上的占位符.这里面会依次
    // 遍历BoundSql的ParameterMapping集合, 取到它们对应的参数值, 通过jdbc的
    // PreparedStatement的各种setxx()方法赋值, 注意下标从1开始...其中赋值操作有交由
    // mybatis的组件TypeHandler来完成, 每种不同的JdbcType都会有一个与其对应的TypeHandler
    // (这是因为jdbc的PreparedStatement太坑爹, 它的setxxx()方法需要区分不同类型的参数)
    // 所以mybatis才会对每一种参数创建一个TypeHandler..我猜是这样子
    handler.parameterize(stmt);
    return stmt;
  }

}

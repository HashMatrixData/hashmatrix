/**
 * 公共测试 fixtures / Mock 数据约定。
 *
 * <p>各 Java 子仓的测试**复用**此包，避免各自重复造 Mock：
 * <ul>
 *   <li>租户一律取 {@link io.hashmatrix.test.fixtures.MockTenants}（{@code acme}/{@code beta}/{@code tenant-demo}）。</li>
 *   <li>样例数据一律经 {@link io.hashmatrix.test.fixtures.MockData} 生成，确定性、虚构、脱敏。</li>
 *   <li>新增 fixtures 必须满足信息红线：邮箱 {@code @example.com}、主机 {@code *.example.internal}、
 *       禁用任何真实甲方标识（见主仓 {@code CLAUDE.md}）。</li>
 * </ul>
 */
package io.hashmatrix.test.fixtures;

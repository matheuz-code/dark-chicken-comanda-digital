package br.edu.unasp.comandadigital.service;

import org.junit.jupiter.api.Test;
import java.util.Optional;
import br.edu.unasp.comandadigital.entity.*;
import br.edu.unasp.comandadigital.entity.enums.*;
import br.edu.unasp.comandadigital.repository.*;
import br.edu.unasp.comandadigital.dto.admin.pedido.AtualizarStatusPedidoRequest;
import br.edu.unasp.comandadigital.exception.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class RecuperacaoPedidoTest {
    @Test void baixaEstoqueAntesDeConfirmarPedido() {
        var usuarios = mock(UsuarioRepository.class); var pedidos = mock(PedidoRepository.class);
        var itens = mock(PedidoItemRepository.class); var historico = mock(PedidoStatusHistoricoRepository.class);
        var estoque = mock(EstoquePedidoService.class); var usuario = usuario(PerfilUsuario.ADMIN);
        var pedido = mock(Pedido.class);
        when(usuarios.findByEmailIgnoreCase("admin@teste.com")).thenReturn(Optional.of(usuario));
        when(pedidos.findByIdForUpdate(42L)).thenReturn(Optional.of(pedido));
        when(pedido.getStatus()).thenReturn(StatusPedido.RECEBIDO); when(pedido.getCliente()).thenReturn(usuario);
        new AdminPedidoService(pedidos, itens, historico, usuarios, estoque)
            .atualizarStatus("admin@teste.com", 42L, new AtualizarStatusPedidoRequest(StatusPedido.CONFIRMADO, null));
        var ordem = inOrder(estoque, pedido, pedidos);
        ordem.verify(estoque).baixarAoConfirmar(pedido, usuario);
        ordem.verify(pedido).setStatus(StatusPedido.CONFIRMADO);
        ordem.verify(pedidos).saveAndFlush(pedido);
    }
    @Test void falhaDeEstoqueNaoAvancaEstado() {
        var usuarios = mock(UsuarioRepository.class); var pedidos = mock(PedidoRepository.class);
        var estoque = mock(EstoquePedidoService.class); var usuario = usuario(PerfilUsuario.ADMIN); var pedido = mock(Pedido.class);
        when(usuarios.findByEmailIgnoreCase("admin@teste.com")).thenReturn(Optional.of(usuario));
        when(pedidos.findByIdForUpdate(42L)).thenReturn(Optional.of(pedido)); when(pedido.getStatus()).thenReturn(StatusPedido.RECEBIDO);
        doThrow(new RegraNegocioException("Estoque insuficiente")).when(estoque).baixarAoConfirmar(pedido, usuario);
        var service = new AdminPedidoService(pedidos, mock(PedidoItemRepository.class), mock(PedidoStatusHistoricoRepository.class), usuarios, estoque);
        assertThatThrownBy(() -> service.atualizarStatus("admin@teste.com", 42L, new AtualizarStatusPedidoRequest(StatusPedido.CONFIRMADO, null))).isInstanceOf(RegraNegocioException.class);
        verify(pedido, never()).setStatus(any()); verify(pedidos, never()).saveAndFlush(any());
    }
    @Test void detalheDeOutroClienteNaoEExposto() {
        var usuarios = mock(UsuarioRepository.class); var pedidos = mock(PedidoRepository.class); var usuario = usuario(PerfilUsuario.CLIENTE);
        when(usuario.getId()).thenReturn(5L); when(usuarios.findByEmailIgnoreCase("cliente@teste.com")).thenReturn(Optional.of(usuario));
        when(pedidos.findByIdAndClienteId(42L, 5L)).thenReturn(Optional.empty());
        var service = new PedidoService(usuarios, mock(PratoRepository.class), pedidos, mock(PedidoItemRepository.class), mock(PedidoStatusHistoricoRepository.class), mock(NecessidadeEstoqueService.class));
        assertThatThrownBy(() -> service.detalharMeu("cliente@teste.com", 42L)).isInstanceOf(RecursoNaoEncontradoException.class);
        verify(pedidos, never()).findById(any());
    }
    @Test void detalheDoProprietarioUsaItensDoBanco() {
        var usuarios = mock(UsuarioRepository.class); var pedidos = mock(PedidoRepository.class); var itens = mock(PedidoItemRepository.class); var usuario = usuario(PerfilUsuario.CLIENTE); var pedido = mock(Pedido.class);
        when(usuario.getId()).thenReturn(5L); when(pedido.getId()).thenReturn(42L);
        when(usuarios.findByEmailIgnoreCase("cliente@teste.com")).thenReturn(Optional.of(usuario));
        when(pedidos.findByIdAndClienteId(42L, 5L)).thenReturn(Optional.of(pedido));
        var service = new PedidoService(usuarios, mock(PratoRepository.class), pedidos, itens, mock(PedidoStatusHistoricoRepository.class), mock(NecessidadeEstoqueService.class));
        assertThat(service.detalharMeu("cliente@teste.com", 42L).id()).isEqualTo(42L); verify(itens).findByPedidoId(42L);
    }
    private Usuario usuario(PerfilUsuario perfil) { var u = mock(Usuario.class); when(u.getPerfil()).thenReturn(perfil); when(u.getStatus()).thenReturn(StatusAtivacao.ATIVO); return u; }
}

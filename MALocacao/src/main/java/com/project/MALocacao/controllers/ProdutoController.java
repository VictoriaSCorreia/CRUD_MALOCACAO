package com.project.MALocacao.controllers;

import com.project.MALocacao.dtos.ProdutoDto;
import com.project.MALocacao.models.ProdutoModel;
import com.project.MALocacao.services.EntradaService;
import com.project.MALocacao.services.ProdutoService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/produto")
public class ProdutoController {

    final ProdutoService produtoService;
    final EntradaService entradaService; 

    public ProdutoController(ProdutoService produtoService, EntradaService entradaService) {
        this.produtoService = produtoService;
        this.entradaService = entradaService;
    }

    @PostMapping
    public ResponseEntity<Object> saveProduto(@RequestBody @Valid ProdutoDto produtoDto){
        // Confere se o produto já existe pelo nome(unique)
        if(produtoService.existsByNome(produtoDto.getNome())){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Esse tipo de produto já existe!");
        }

        // Pega as informações do DTO que veio no corpo da requisição e altera o ProdutoModel
        var produtoModel = new ProdutoModel();
        BeanUtils.copyProperties(produtoDto, produtoModel);

        // Salva
        return ResponseEntity.status(HttpStatus.CREATED).body(produtoService.save(produtoModel));
    }

    @GetMapping
    public ResponseEntity<Page<ProdutoModel>> getAllProdutos(@PageableDefault(page = 0, size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable){
        return ResponseEntity.status(HttpStatus.OK).body(produtoService.findAll(pageable));
    }


    @GetMapping("/{id}")
    public ResponseEntity<Object> getOneProduto(@PathVariable(value = "id") Long id){
        Optional<ProdutoModel> produtoModelOptional = produtoService.findById(id);
        if (!produtoModelOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produto não encontrado.");
        }
        return ResponseEntity.status(HttpStatus.OK).body(produtoModelOptional.get());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteProduto(@PathVariable(value = "id") Long id) {
        Optional<ProdutoModel> produtoModelOptional = produtoService.findById(id);
        if (!produtoModelOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produto não encontrado.");
        }
        if (entradaService.existsByProdutoId(id)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Não é possível excluir este produto porque ele possui entradas associadas.");
        }
        produtoService.delete(produtoModelOptional.get());
        return ResponseEntity.status(HttpStatus.OK).body("Produto deletado.");
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updateProduto(@PathVariable(value = "id") Long id,
                                                    @RequestBody @Valid ProdutoDto produtoDto){
        Optional<ProdutoModel> produtoModelOptional = produtoService.findById(id);
        if (!produtoModelOptional.isPresent()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Produto não encontrado.");
        }
        if (produtoService.existsByNome(produtoDto.getNome()) && !produtoModelOptional.get().getNome().equals(produtoDto.getNome())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Esse nome de produto já está em uso por outro produto.");
        }
        if (produtoDto.getNumUnidades() != produtoModelOptional.get().getNumUnidades()){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("O número de unidades em estoque só pode ser alterado através da Entrada e Saída.");
        }

        var produtoModel = produtoModelOptional.get();

        // Pega as informações do DTO que veio no corpo da requisição e altera o ProdutoModel
        BeanUtils.copyProperties(produtoDto, produtoModel);

        // Precisa setar o ID manualmente pois o DTO não possui esse campo(ele é gerado automaticamente no Model)
        produtoModel.setId(produtoModelOptional.get().getId());

        // Salva
        return ResponseEntity.status(HttpStatus.OK).body(produtoService.save(produtoModel));
    }

}

